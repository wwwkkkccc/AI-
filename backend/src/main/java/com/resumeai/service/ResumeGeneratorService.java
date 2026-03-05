package com.resumeai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.dto.AnalyzeResponse;
import com.resumeai.dto.GeneratedResumeResponse;
import com.resumeai.model.AnalysisRecord;
import com.resumeai.model.UserAccount;
import com.resumeai.repository.AnalysisRecordRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeGeneratorService {

    private final AnalysisRecordRepository analysisRecordRepository;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public ResumeGeneratorService(
            AnalysisRecordRepository analysisRecordRepository,
            LlmClient llmClient,
            ObjectMapper objectMapper) {
        this.analysisRecordRepository = analysisRecordRepository;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public GeneratedResumeResponse generateFromJd(String targetRole, String jdText, String userBackground) {
        String role = clean(targetRole);
        String jd = clean(jdText);
        String background = clean(userBackground);
        if (role.isEmpty()) {
            throw new IllegalArgumentException("target_role is required");
        }
        if (jd.length() < 20) {
            throw new IllegalArgumentException("jd_text is required");
        }

        String prompt = """
                You are a senior resume writer.
                Generate a complete Chinese resume in Markdown for role: %s.
                Use JD requirements and user background to tailor the content.
                Keep it realistic. If a fact is missing, mark as [待补充] instead of inventing.

                Output sections in this order:
                1. 标题与联系方式
                2. 职业摘要
                3. 核心技能
                4. 工作经历（每段 3-4 条 STAR 风格，含量化）
                5. 项目经历（2-3 个）
                6. 教育背景
                7. 证书/附加信息（可选）

                JD:
                %s

                User background:
                %s
                """.formatted(role, cut(jd, 4500), cut(background, 3200));

        String generated = llmClient.chat(List.of(
                Map.of("role", "system", "content", "You are an ATS-optimized resume writer. Output markdown only."),
                Map.of("role", "user", "content", prompt)
        ), 0.25, 55);

        GeneratedResumeResponse response = new GeneratedResumeResponse();
        response.setMode("GENERATE");
        response.setModelUsed(generated != null);
        response.setMarkdown(generated != null ? generated : fallbackGenerate(role, jd, background));
        return response;
    }

    @Transactional(readOnly = true)
    public GeneratedResumeResponse rewriteByAnalysis(Long analysisId, UserAccount user, boolean adminMode) {
        if (analysisId == null) {
            throw new IllegalArgumentException("analysis_id is required");
        }
        AnalysisRecord record = analysisRecordRepository.findById(analysisId)
                .orElseThrow(() -> new IllegalArgumentException("analysis record not found"));
        verifyRecordAccess(record, user, adminMode);
        return rewriteInternal(
                record.getResumeText(),
                record.getJdText(),
                record.getTargetRole(),
                record.getResultJson(),
                analysisId
        );
    }

    @Transactional(readOnly = true)
    public GeneratedResumeResponse rewriteByRawText(String resumeText, String jdText, String targetRole) {
        String resume = clean(resumeText);
        if (resume.length() < 30) {
            throw new IllegalArgumentException("resume text is too short");
        }
        return rewriteInternal(resume, jdText, targetRole, "", null);
    }

    private GeneratedResumeResponse rewriteInternal(
            String resumeText,
            String jdText,
            String targetRole,
            String resultJson,
            Long analysisId) {
        String resume = clean(resumeText);
        String jd = clean(jdText);
        String role = clean(targetRole);

        if (resume.length() < 30) {
            throw new IllegalArgumentException("resume text is too short");
        }
        if (role.isEmpty()) {
            role = inferRoleFromJd(jd);
        }

        String analysisHints = extractAnalysisHints(resultJson);
        String prompt = """
                Rewrite the resume into a complete Chinese Markdown resume for role: %s.
                Keep facts faithful to original resume. Do not fabricate employers, durations, or achievements.
                Use STAR style bullets for experience.
                Improve wording and quantification.
                Add missing skills aligned with JD only if they are supported by resume context.

                Extra optimization hints:
                %s

                Original resume:
                %s

                JD:
                %s
                """.formatted(role, cut(analysisHints, 1500), cut(resume, 6000), cut(jd, 4000));

        String rewritten = llmClient.chat(List.of(
                Map.of("role", "system", "content", "You are a resume rewriting expert. Output markdown only."),
                Map.of("role", "user", "content", prompt)
        ), 0.2, 55);

        GeneratedResumeResponse response = new GeneratedResumeResponse();
        response.setMode("REWRITE");
        response.setModelUsed(rewritten != null);
        response.setAnalysisId(analysisId);
        response.setMarkdown(rewritten != null ? rewritten : fallbackRewrite(resume, jd, role));
        return response;
    }

    private String fallbackGenerate(String role, String jdText, String background) {
        List<String> topRequirements = extractTopRequirements(jdText, 8);
        return """
                # [候选人姓名]
                - 目标岗位：%s
                - 联系方式：[待补充]

                ## 职业摘要
                具备与 %s 岗位相关的交付经验，能够围绕业务目标进行系统设计、开发与优化，持续提升稳定性与效率。

                ## 核心技能
                %s

                ## 工作经历
                ### [公司名称] | [职位] | [时间]
                - S：面对 [业务场景] 的性能与稳定性问题。
                - T：负责端到端方案设计与落地。
                - A：主导 [技术方案]，完成关键链路改造与发布治理。
                - R：核心指标提升 [待补充%%]，故障率下降 [待补充%%]。

                ## 项目经历
                ### [项目名称]
                - 基于 JD 要求构建项目说明，补充架构、挑战、取舍与结果。

                ## 教育背景
                - [学校] | [专业] | [学历]

                ## 补充信息
                - 用户背景：%s
                """.formatted(role, role, toBulletList(topRequirements), cut(background, 1200));
    }

    private String fallbackRewrite(String resumeText, String jdText, String role) {
        List<String> experienceLines = pickExperienceLines(resumeText, 6);
        List<String> topRequirements = extractTopRequirements(jdText, 8);

        StringBuilder sb = new StringBuilder();
        sb.append("# [候选人姓名]\n");
        sb.append("- 目标岗位：").append(role).append("\n");
        sb.append("- 联系方式：[请补充]\n\n");
        sb.append("## 职业摘要\n");
        sb.append("围绕 ").append(role).append(" 岗位要求，突出项目交付、性能优化、稳定性治理与跨团队协作能力。\n\n");

        sb.append("## 核心技能\n");
        for (String req : topRequirements) {
            sb.append("- ").append(req).append("\n");
        }
        if (topRequirements.isEmpty()) {
            sb.append("- [根据 JD 补充核心技能]\n");
        }

        sb.append("\n## 工作经历（STAR 重写）\n");
        if (experienceLines.isEmpty()) {
            sb.append("- S：在 [业务场景] 中遇到 [问题]。\n");
            sb.append("- T：负责 [目标]。\n");
            sb.append("- A：采用 [方案] 解决 [难点]。\n");
            sb.append("- R：达成 [量化结果]。\n");
        } else {
            int idx = 1;
            for (String line : experienceLines) {
                sb.append("### 经历 ").append(idx++).append("\n");
                sb.append("- S/T：").append(line).append("\n");
                sb.append("- A：补充技术选型、关键动作和协作方式。\n");
                sb.append("- R：补充可量化业务结果（效率/成本/稳定性）。\n");
            }
        }

        sb.append("\n## 项目经历\n");
        sb.append("- 结合 JD 重点，补充 2-3 个最相关项目，强调架构与业务价值。\n\n");
        sb.append("## 教育背景\n");
        sb.append("- [按原简历补充]\n");
        return sb.toString();
    }

    private List<String> pickExperienceLines(String resumeText, int limit) {
        String[] lines = resumeText.split("\\n+");
        List<String> out = new ArrayList<>();
        for (String raw : lines) {
            String line = clean(raw);
            if (line.length() < 14) {
                continue;
            }
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("project") || lower.contains("项目") || lower.contains("负责")
                    || lower.contains("优化") || lower.contains("提升")) {
                out.add(cut(line, 180));
            }
            if (out.size() >= limit) {
                break;
            }
        }
        return out;
    }

    private String extractAnalysisHints(String resultJson) {
        String json = clean(resultJson);
        if (json.isEmpty()) {
            return "";
        }
        try {
            AnalyzeResponse parsed = objectMapper.readValue(json, AnalyzeResponse.class);
            String summary = parsed.getOptimized() == null ? "" : clean(parsed.getOptimized().getSummary());
            List<String> missing = parsed.getMissingKeywords() == null ? List.of() : parsed.getMissingKeywords();
            return "summary=" + summary + "; missing_keywords=" + missing;
        } catch (Exception ex) {
            return "";
        }
    }

    private String inferRoleFromJd(String jdText) {
        String lower = clean(jdText).toLowerCase(Locale.ROOT);
        if (lower.contains("backend") || lower.contains("后端")) {
            return "后端工程师";
        }
        if (lower.contains("frontend") || lower.contains("前端")) {
            return "前端工程师";
        }
        if (lower.contains("product") || lower.contains("产品")) {
            return "产品经理";
        }
        return "工程师";
    }

    private List<String> extractTopRequirements(String jdText, int limit) {
        String[] lines = clean(jdText).split("\\n+");
        List<String> out = new ArrayList<>();
        for (String raw : lines) {
            String line = clean(raw);
            if (line.length() < 4 || line.length() > 80) {
                continue;
            }
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("要求") || lower.contains("熟悉") || lower.contains("must")
                    || lower.contains("负责") || lower.contains("经验")) {
                out.add("- " + cut(line, 70));
            }
            if (out.size() >= limit) {
                break;
            }
        }
        return out;
    }

    private String toBulletList(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "- [按 JD 补充技能标签]";
        }
        return String.join("\n", lines);
    }

    private void verifyRecordAccess(AnalysisRecord record, UserAccount user, boolean adminMode) {
        if (adminMode) {
            return;
        }
        if (!record.getUserId().equals(user.getId())) {
            throw new ForbiddenException("cannot access this analysis record");
        }
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
}
