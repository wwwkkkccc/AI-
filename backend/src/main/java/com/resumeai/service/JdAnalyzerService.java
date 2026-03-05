package com.resumeai.service;

import com.resumeai.dto.JdAnalyzeRequest;
import com.resumeai.dto.JdDimension;
import com.resumeai.dto.JdRadarResponse;
import com.resumeai.model.AnalysisRecord;
import com.resumeai.model.UserAccount;
import com.resumeai.repository.AnalysisRecordRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JdAnalyzerService {

    private static final Pattern YEARS_PATTERN = Pattern.compile("(\\d{1,2})\\s*(?:\\+)?\\s*(?:years?|年)");

    private static final List<String> TECH_KEYWORDS = List.of(
            "java", "spring", "mysql", "redis", "kafka", "docker", "kubernetes",
            "python", "go", "vue", "react", "node", "微服务", "分布式", "sql", "linux"
    );
    private static final List<String> SOFT_SKILLS = List.of(
            "沟通", "协作", "推动", "ownership", "communication", "leadership", "责任心", "抗压"
    );
    private static final List<String> DOMAIN_WORDS = List.of(
            "电商", "金融", "支付", "广告", "教育", "医疗", "互联网", "to b", "to c", "saas"
    );
    private static final List<String> COMPLEXITY_WORDS = List.of(
            "架构", "高并发", "高可用", "分布式", "性能", "稳定性", "容灾", "治理", "tps", "qps"
    );

    private final AnalysisRecordRepository analysisRecordRepository;

    public JdAnalyzerService(AnalysisRecordRepository analysisRecordRepository) {
        this.analysisRecordRepository = analysisRecordRepository;
    }

    @Transactional(readOnly = true)
    public JdRadarResponse analyze(JdAnalyzeRequest req, UserAccount user, boolean adminMode) {
        ResolvedContext context = resolveContext(req, user, adminMode);
        String jd = context.jdText;
        String resume = context.resumeText;

        Set<String> techFromJd = extractContainedKeywords(jd, TECH_KEYWORDS);
        Set<String> softFromJd = extractContainedKeywords(jd, SOFT_SKILLS);
        Integer requiredYears = extractYears(jd);
        Integer resumeYears = extractYears(resume);

        double techScore = ratioScore(techFromJd, resume);
        double softScore = ratioScore(softFromJd, resume);
        double yearsScore = yearsMatchScore(requiredYears, resumeYears);
        double complexityScore = ratioScore(extractContainedKeywords(jd, COMPLEXITY_WORDS), resume);
        double domainScore = ratioScore(extractContainedKeywords(jd, DOMAIN_WORDS), resume);
        double eduScore = educationScore(jd, resume);

        List<JdDimension> dimensions = new ArrayList<>();
        dimensions.add(buildDimension("技术栈匹配", techScore,
                techFromJd.isEmpty() ? "JD 未抽取到明确技术栈" : "JD 技术要求: " + techFromJd));
        dimensions.add(buildDimension("经验年限匹配", yearsScore,
                "JD 年限要求: " + (requiredYears == null ? "未明确" : requiredYears + "年")
                        + "；简历推测: " + (resumeYears == null ? "未明确" : resumeYears + "年")));
        dimensions.add(buildDimension("项目复杂度匹配", complexityScore,
                "基于架构/高并发/稳定性等关键词进行匹配"));
        dimensions.add(buildDimension("软技能匹配", softScore,
                softFromJd.isEmpty() ? "JD 未抽取到明确软技能" : "JD 软技能要求: " + softFromJd));
        dimensions.add(buildDimension("行业背景匹配", domainScore, "基于行业域词做匹配"));
        dimensions.add(buildDimension("教育背景匹配", eduScore, "基于学历关键词做匹配"));

        double overall = round2(dimensions.stream()
                .mapToDouble(d -> d.getScore() == null ? 0.0 : d.getScore())
                .average()
                .orElse(0.0));

        JdRadarResponse response = new JdRadarResponse();
        response.setOverallScore(overall);
        response.setDimensions(dimensions);
        response.setExtractedTechStack(new ArrayList<>(techFromJd));
        response.setExtractedSoftSkills(new ArrayList<>(softFromJd));
        response.setExtractedYearsRequirement(requiredYears == null ? "未明确" : requiredYears + "年");
        return response;
    }

    private ResolvedContext resolveContext(JdAnalyzeRequest req, UserAccount user, boolean adminMode) {
        String jd = clean(req.getJdText());
        String resume = clean(req.getResumeText());

        if (req.getAnalysisId() != null) {
            AnalysisRecord record = analysisRecordRepository.findById(req.getAnalysisId())
                    .orElseThrow(() -> new IllegalArgumentException("analysis record not found"));
            if (!adminMode && !record.getUserId().equals(user.getId())) {
                throw new ForbiddenException("cannot access this analysis record");
            }
            if (jd.isEmpty()) {
                jd = clean(record.getJdText());
            }
            if (resume.isEmpty()) {
                resume = clean(record.getResumeText());
            }
        }

        if (jd.length() < 20) {
            throw new IllegalArgumentException("jd_text is required");
        }
        if (resume.length() < 20) {
            throw new IllegalArgumentException("resume text is too short");
        }

        ResolvedContext context = new ResolvedContext();
        context.jdText = jd;
        context.resumeText = resume;
        return context;
    }

    private Set<String> extractContainedKeywords(String text, List<String> dictionary) {
        String lower = clean(text).toLowerCase(Locale.ROOT);
        Set<String> out = new LinkedHashSet<>();
        for (String keyword : dictionary) {
            String k = keyword.toLowerCase(Locale.ROOT);
            if (lower.contains(k)) {
                out.add(keyword);
            }
        }
        return out;
    }

    private double ratioScore(Set<String> requiredKeywords, String resumeText) {
        if (requiredKeywords.isEmpty()) {
            return 72.0;
        }
        String lower = clean(resumeText).toLowerCase(Locale.ROOT);
        int matched = 0;
        for (String k : requiredKeywords) {
            if (lower.contains(k.toLowerCase(Locale.ROOT))) {
                matched++;
            }
        }
        return round2((double) matched / requiredKeywords.size() * 100.0);
    }

    private Integer extractYears(String text) {
        Matcher m = YEARS_PATTERN.matcher(clean(text).toLowerCase(Locale.ROOT));
        Integer max = null;
        while (m.find()) {
            try {
                int years = Integer.parseInt(m.group(1));
                if (max == null || years > max) {
                    max = years;
                }
            } catch (Exception ignore) {
                // ignore malformed years
            }
        }
        return max;
    }

    private double yearsMatchScore(Integer requiredYears, Integer resumeYears) {
        if (requiredYears == null) {
            return 75.0;
        }
        if (resumeYears == null) {
            return 45.0;
        }
        if (resumeYears >= requiredYears) {
            return 95.0;
        }
        if (resumeYears + 1 >= requiredYears) {
            return 80.0;
        }
        if (resumeYears + 2 >= requiredYears) {
            return 65.0;
        }
        return 40.0;
    }

    private double educationScore(String jdText, String resumeText) {
        String jd = clean(jdText).toLowerCase(Locale.ROOT);
        String resume = clean(resumeText).toLowerCase(Locale.ROOT);
        boolean needMaster = jd.contains("硕士") || jd.contains("master");
        boolean needBachelor = jd.contains("本科") || jd.contains("bachelor");

        boolean hasMaster = resume.contains("硕士") || resume.contains("master");
        boolean hasBachelor = resume.contains("本科") || resume.contains("bachelor");

        if (!needMaster && !needBachelor) {
            return 72.0;
        }
        if (needMaster) {
            return hasMaster ? 95.0 : (hasBachelor ? 60.0 : 35.0);
        }
        return (hasBachelor || hasMaster) ? 92.0 : 40.0;
    }

    private JdDimension buildDimension(String name, double score, String detail) {
        JdDimension dimension = new JdDimension();
        dimension.setName(name);
        dimension.setScore(round2(Math.max(0.0, Math.min(100.0, score))));
        dimension.setMaxScore(100.0);
        dimension.setDetail(detail);
        return dimension;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static class ResolvedContext {
        private String jdText;
        private String resumeText;
    }
}
