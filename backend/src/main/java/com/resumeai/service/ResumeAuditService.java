package com.resumeai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.resumeai.dto.ResumeAuditItem;
import com.resumeai.dto.ResumeAuditRequest;
import com.resumeai.dto.ResumeAuditResponse;
import com.resumeai.model.AnalysisRecord;
import com.resumeai.model.UserAccount;
import com.resumeai.repository.AnalysisRecordRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resume authenticity/risk audit service using rule checks plus optional LLM findings.
 */
@Service
public class ResumeAuditService {

    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d{2,5})\\s*%");
    private static final Pattern YEARS_PATTERN = Pattern.compile("(\\d{1,2})\\s*(?:年|years?)");
    private static final Pattern YEAR_RANGE_PATTERN = Pattern.compile("(20\\d{2})\\s*[-~至]\\s*(20\\d{2}|至今|present)");

    private final AnalysisRecordRepository analysisRecordRepository;
    private final LlmClient llmClient;

    public ResumeAuditService(AnalysisRecordRepository analysisRecordRepository, LlmClient llmClient) {
        this.analysisRecordRepository = analysisRecordRepository;
        this.llmClient = llmClient;
    }

    /** Audits one resume context and returns risk score, level, and findings list. */
    @Transactional(readOnly = true)
    public ResumeAuditResponse audit(ResumeAuditRequest req, UserAccount user, boolean adminMode) {
        ResolvedAuditContext context = resolveContext(req, user, adminMode);
        List<ResumeAuditItem> items = new ArrayList<>();

        detectSuspiciousNumbers(context.resumeText, items);
        detectExperienceVsProjects(context.resumeText, items);
        detectTimelineConflicts(context.resumeText, items);
        detectSkillMismatch(context.resumeText, context.targetRole, items);
        mergeLlmFindings(context.resumeText, context.targetRole, items);

        double riskScore = calculateRiskScore(items);
        String riskLevel = riskScore >= 70 ? "HIGH" : riskScore >= 40 ? "MEDIUM" : "LOW";

        ResumeAuditResponse response = new ResumeAuditResponse();
        response.setRiskScore(riskScore);
        response.setRiskLevel(riskLevel);
        response.setSummary(buildSummary(riskLevel, riskScore, items.size()));
        response.setAuditItems(items);
        return response;
    }

    private void detectSuspiciousNumbers(String resumeText, List<ResumeAuditItem> items) {
        Matcher m = PERCENT_PATTERN.matcher(resumeText);
        while (m.find()) {
            int value = parseInt(m.group(1), -1);
            if (value > 300) {
                items.add(item(
                        "数据夸大",
                        "检测到较高百分比指标：" + value + "%，需要确认统计口径与基线。",
                        "HIGH",
                        "补充原始基线、时间范围、样本规模，避免绝对化表达。"
                ));
            }
        }
        String lower = resumeText.toLowerCase(Locale.ROOT);
        if (lower.contains("10000%") || lower.contains("1000%")) {
            items.add(item(
                    "数据夸大",
                    "存在极端百分比表达，可能引发可信度风险。",
                    "HIGH",
                    "将表达调整为可验证区间，并提供监控或报表来源。"
            ));
        }
    }

    private void detectExperienceVsProjects(String resumeText, List<ResumeAuditItem> items) {
        Integer years = extractMaxYears(resumeText);
        int projectCount = countContains(resumeText, "项目") + countContains(resumeText.toLowerCase(Locale.ROOT), "project");
        if (years != null && years <= 3 && projectCount >= 10) {
            items.add(item(
                    "经历一致性",
                    "简历显示经验年限较短，但项目数量非常多（约 " + projectCount + " 项）。",
                    "MEDIUM",
                    "区分主导项目与参与项目，保留 3-5 个代表性项目并注明角色深度。"
            ));
        }
    }

    private void detectTimelineConflicts(String resumeText, List<ResumeAuditItem> items) {
        Matcher matcher = YEAR_RANGE_PATTERN.matcher(resumeText);
        while (matcher.find()) {
            int start = parseInt(matcher.group(1), 0);
            String endText = matcher.group(2).toLowerCase(Locale.ROOT);
            int end = "present".equals(endText) || "至今".equals(endText) ? 2100 : parseInt(endText, 0);
            if (end > 0 && start > end) {
                items.add(item(
                        "时间线矛盾",
                        "检测到时间范围可能倒置：" + matcher.group(0),
                        "HIGH",
                        "核对起止时间并统一格式（例如 2022.03-2024.06）。"
                ));
            }
        }
    }

    private void detectSkillMismatch(String resumeText, String targetRole, List<ResumeAuditItem> items) {
        String lower = resumeText.toLowerCase(Locale.ROOT);
        int expertClaims = countContains(lower, "精通") + countContains(lower, "expert") + countContains(lower, "proficient");
        int majorTechCount = countContainsAny(lower, List.of("java", "python", "go", "kafka", "redis", "kubernetes", "mysql"));
        if (expertClaims >= 4 && majorTechCount >= 8) {
            items.add(item(
                    "技能可信度",
                    "高阶技能声明较多，建议核验是否都有项目证据支撑。",
                    "MEDIUM",
                    "在每个核心技能后附一个对应项目成果或问题场景。"
            ));
        }
        if (!clean(targetRole).isEmpty()) {
            String roleLower = targetRole.toLowerCase(Locale.ROOT);
            if (roleLower.contains("后端") && lower.contains("react") && !lower.contains("java") && !lower.contains("spring")) {
                items.add(item(
                        "岗位匹配",
                        "目标岗位偏后端，但核心项目描述中后端技术证据较弱。",
                        "LOW",
                        "补充后端服务设计、数据库、性能优化相关的实战案例。"
                ));
            }
        }
    }

    private void mergeLlmFindings(String resumeText, String targetRole, List<ResumeAuditItem> items) {
        String prompt = """
                请检查下面简历是否存在真实性风险或描述夸大。
                输出严格 JSON：
                {
                  "items": [
                    {"category":"...", "description":"...", "severity":"LOW|MEDIUM|HIGH", "suggestion":"..."}
                  ]
                }
                最多返回 3 条。不要杜撰候选人没有写过的信息。

                target_role: %s
                resume_text:
                %s
                """.formatted(clean(targetRole), cut(resumeText, 5000));

        String raw = llmClient.chat(List.of(
                Map.of("role", "system", "content", "You are a resume audit expert. Return strict JSON only."),
                Map.of("role", "user", "content", prompt)
        ), 0.15, 45);
        JsonNode root = llmClient.parseJsonObject(raw);
        if (root == null || !root.path("items").isArray()) {
            return;
        }

        for (JsonNode node : root.path("items")) {
            String category = clean(node.path("category").asText(""));
            String description = clean(node.path("description").asText(""));
            String severity = normalizeSeverity(node.path("severity").asText(""));
            String suggestion = clean(node.path("suggestion").asText(""));
            if (description.isEmpty()) {
                continue;
            }
            String key = (category + "|" + description).toLowerCase(Locale.ROOT);
            boolean exists = items.stream().anyMatch(i ->
                    ((clean(i.getCategory()) + "|" + clean(i.getDescription())).toLowerCase(Locale.ROOT)).equals(key)
            );
            if (!exists) {
                items.add(item(
                        category.isEmpty() ? "逻辑一致性" : category,
                        cut(description, 220),
                        severity,
                        suggestion.isEmpty() ? "补充可验证证据，避免绝对化结论。" : cut(suggestion, 220)
                ));
            }
        }
    }

    private ResolvedAuditContext resolveContext(ResumeAuditRequest req, UserAccount user, boolean adminMode) {
        String resumeText = clean(req.getResumeText());
        String targetRole = clean(req.getTargetRole());

        if (req.getAnalysisId() != null) {
            AnalysisRecord record = analysisRecordRepository.findById(req.getAnalysisId())
                    .orElseThrow(() -> new IllegalArgumentException("analysis record not found"));
            if (!adminMode && !record.getUserId().equals(user.getId())) {
                throw new ForbiddenException("cannot access this analysis record");
            }
            if (resumeText.isEmpty()) {
                resumeText = clean(record.getResumeText());
            }
            if (targetRole.isEmpty()) {
                targetRole = clean(record.getTargetRole());
            }
        }

        if (resumeText.length() < 30) {
            throw new IllegalArgumentException("resume text is too short");
        }
        ResolvedAuditContext out = new ResolvedAuditContext();
        out.resumeText = resumeText;
        out.targetRole = targetRole;
        return out;
    }

    private ResumeAuditItem item(String category, String description, String severity, String suggestion) {
        ResumeAuditItem item = new ResumeAuditItem();
        item.setCategory(category);
        item.setDescription(description);
        item.setSeverity(normalizeSeverity(severity));
        item.setSuggestion(suggestion);
        return item;
    }

    private double calculateRiskScore(List<ResumeAuditItem> items) {
        double score = 0.0;
        for (ResumeAuditItem item : items) {
            String severity = normalizeSeverity(item.getSeverity());
            if ("HIGH".equals(severity)) {
                score += 28.0;
            } else if ("MEDIUM".equals(severity)) {
                score += 16.0;
            } else {
                score += 8.0;
            }
        }
        return round2(Math.min(100.0, score));
    }

    private String buildSummary(String level, double score, int count) {
        return "真实性风险等级: " + level + "，风险分: " + score + "，发现可疑点: " + count + " 条。";
    }

    private String normalizeSeverity(String value) {
        String v = clean(value).toUpperCase(Locale.ROOT);
        if ("HIGH".equals(v) || "MEDIUM".equals(v) || "LOW".equals(v)) {
            return v;
        }
        return "MEDIUM";
    }

    private Integer extractMaxYears(String text) {
        Matcher m = YEARS_PATTERN.matcher(clean(text).toLowerCase(Locale.ROOT));
        Integer max = null;
        while (m.find()) {
            int v = parseInt(m.group(1), -1);
            if (v < 0) {
                continue;
            }
            if (max == null || v > max) {
                max = v;
            }
        }
        return max;
    }

    private int countContains(String text, String keyword) {
        int count = 0;
        int idx = 0;
        while (idx >= 0) {
            idx = text.indexOf(keyword, idx);
            if (idx >= 0) {
                count++;
                idx += keyword.length();
            }
        }
        return count;
    }

    private int countContainsAny(String text, List<String> keywords) {
        int count = 0;
        for (String k : keywords) {
            if (text.contains(k)) {
                count++;
            }
        }
        return count;
    }

    private int parseInt(String value, int defaultVal) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return defaultVal;
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cut(String value, int max) {
        String text = clean(value);
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }

    private static class ResolvedAuditContext {
        private String resumeText;
        private String targetRole;
    }
}
