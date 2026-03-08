package com.resumeai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.dto.AnalyzeResponse;
import com.resumeai.dto.GeneratedResumeResponse;
import com.resumeai.model.AnalysisRecord;
import com.resumeai.model.UserAccount;
import com.resumeai.repository.AnalysisRecordRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resume generation and rewrite service using JD context and optional analysis artifacts.
 */
@Service
public class ResumeGeneratorService {

    private static final DateTimeFormatter VERSION_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AnalysisRecordRepository analysisRecordRepository;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final ResumeVersionService resumeVersionService;

    public ResumeGeneratorService(
            AnalysisRecordRepository analysisRecordRepository,
            LlmClient llmClient,
            ObjectMapper objectMapper,
            ResumeVersionService resumeVersionService) {
        this.analysisRecordRepository = analysisRecordRepository;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.resumeVersionService = resumeVersionService;
    }

    /** Generates a fresh markdown resume from role/JD/background input. */
    @Transactional(readOnly = true)
    public GeneratedResumeResponse generateFromJd(String targetRole, String jdText, String userBackground, Long userId) {
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
                Generate a complete resume in Markdown for role: %s.
                Use JD requirements and user background to tailor content.
                Keep it realistic. If any fact is unknown, write [TO_BE_FILLED] instead of inventing.

                Output sections in this order:
                1. Header and contact
                2. Professional summary
                3. Core skills
                4. Work experience (each role 3-4 STAR bullets with metrics)
                5. Project experience (2-3 projects)
                6. Education
                7. Certifications / Additional information (optional)

                JD:
                %s

                User background:
                %s
                """.formatted(role, cut(jd, 4500), cut(background, 3200));

        String generated = llmClient.chat(List.of(
                Map.of("role", "system", "content", "You are an ATS-optimized resume writer. Output markdown only."),
                Map.of("role", "user", "content", prompt)
        ), 0.25, 55);

        String markdown = generated != null ? generated : fallbackGenerate(role, jd, background);

        if (userId != null) {
            saveVersionQuietly(userId, "Generated Resume - " + role, markdown, role, "GENERATED", null);
        }

        GeneratedResumeResponse response = new GeneratedResumeResponse();
        response.setMode("GENERATE");
        response.setModelUsed(generated != null);
        response.setMarkdown(markdown);
        return response;
    }

    /** Rewrites resume based on one stored analysis record and access control context. */
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
                analysisId,
                user.getId()
        );
    }

    /** Rewrites resume from raw resume/JD text without requiring an analysis id. */
    @Transactional(readOnly = true)
    public GeneratedResumeResponse rewriteByRawText(String resumeText, String jdText, String targetRole, Long userId) {
        String resume = clean(resumeText);
        if (resume.length() < 30) {
            throw new IllegalArgumentException("resume text is too short");
        }
        return rewriteInternal(resume, jdText, targetRole, "", null, userId);
    }

    private GeneratedResumeResponse rewriteInternal(
            String resumeText,
            String jdText,
            String targetRole,
            String resultJson,
            Long analysisId,
            Long userId) {
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
                Rewrite the resume into a complete Markdown resume for role: %s.
                Keep facts faithful to the original resume.
                Do not fabricate employers, durations, or achievements.
                Use STAR style bullets and improve quantification.
                Add skills from JD only when supported by resume context.

                Optimization hints:
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

        String markdown = rewritten != null ? rewritten : fallbackRewrite(resume, jd, role);

        if (userId != null) {
            saveVersionQuietly(userId, "Rewritten Resume - " + role, markdown, role, "REWRITTEN", analysisId);
        }

        GeneratedResumeResponse response = new GeneratedResumeResponse();
        response.setMode("REWRITE");
        response.setModelUsed(rewritten != null);
        response.setAnalysisId(analysisId);
        response.setMarkdown(markdown);
        return response;
    }

    private void saveVersionQuietly(Long userId, String titlePrefix, String markdown, String role, String sourceType, Long sourceId) {
        try {
            String title = titlePrefix + " - " + LocalDateTime.now().format(VERSION_TIME_FORMAT);
            resumeVersionService.saveVersion(userId, title, markdown, role, sourceType, sourceId);
        } catch (Exception ignore) {
            // Version persistence should not block main response.
        }
    }

    private String fallbackGenerate(String role, String jdText, String background) {
        List<String> topRequirements = extractTopRequirements(jdText, 8);
        return """
                # [Candidate Name]
                - Target Role: %s
                - Contact: [TO_BE_FILLED]

                ## Professional Summary
                Experienced in delivering production systems for %s role expectations.
                Focused on architecture quality, performance, and reliable delivery.

                ## Core Skills
                %s

                ## Work Experience
                ### [Company Name] | [Role] | [Date Range]
                - Situation: [Business context]
                - Task: [Target and ownership]
                - Action: [Technical approach and execution]
                - Result: [Measured impact, e.g. latency -30%%, reliability +20%%]

                ## Project Experience
                ### [Project Name]
                - Scope, architecture, key trade-offs, and measurable outcomes.

                ## Education
                - [School] | [Major] | [Degree]

                ## Additional Information
                - Background notes: %s
                """.formatted(role, role, toBulletList(topRequirements), cut(background, 1200));
    }

    private String fallbackRewrite(String resumeText, String jdText, String role) {
        List<String> experienceLines = pickExperienceLines(resumeText, 6);
        List<String> topRequirements = extractTopRequirements(jdText, 8);

        StringBuilder sb = new StringBuilder();
        sb.append("# [Candidate Name]\n");
        sb.append("- Target Role: ").append(role).append("\n");
        sb.append("- Contact: [TO_BE_FILLED]\n\n");

        sb.append("## Professional Summary\n");
        sb.append("Aligned to ").append(role).append(" role requirements with clear delivery impact and maintainable engineering practices.\n\n");

        sb.append("## Core Skills\n");
        if (topRequirements.isEmpty()) {
            sb.append("- Map JD requirements into proven skills with project evidence.\n");
        } else {
            for (String req : topRequirements) {
                sb.append("- ").append(req).append("\n");
            }
        }

        sb.append("\n## Work Experience (STAR)\n");
        if (experienceLines.isEmpty()) {
            sb.append("- Situation: [Business context]\n");
            sb.append("- Task: [Goal and ownership]\n");
            sb.append("- Action: [Solution and execution details]\n");
            sb.append("- Result: [Measurable outcome]\n");
        } else {
            int idx = 1;
            for (String line : experienceLines) {
                sb.append("### Experience ").append(idx++).append("\n");
                sb.append("- Situation/Task: ").append(line).append("\n");
                sb.append("- Action: Add architecture choices, constraints, and trade-offs.\n");
                sb.append("- Result: Add measurable business or engineering outcomes.\n");
            }
        }

        sb.append("\n## Project Experience\n");
        sb.append("- Add 2-3 projects with architecture, decisions, and impact metrics.\n\n");
        sb.append("## Education\n");
        sb.append("- [Fill from original resume]\n");
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
            if (lower.contains("project")
                    || lower.contains("deliver")
                    || lower.contains("optimiz")
                    || lower.contains("improv")
                    || lower.contains("responsible")) {
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
        if (lower.contains("backend")) {
            return "Backend Engineer";
        }
        if (lower.contains("frontend")) {
            return "Frontend Engineer";
        }
        if (lower.contains("product")) {
            return "Product Manager";
        }
        return "Software Engineer";
    }

    private List<String> extractTopRequirements(String jdText, int limit) {
        String[] lines = clean(jdText).split("\\n+");
        List<String> out = new ArrayList<>();
        for (String raw : lines) {
            String line = clean(raw);
            if (line.length() < 4 || line.length() > 100) {
                continue;
            }
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("require")
                    || lower.contains("must")
                    || lower.contains("experience")
                    || lower.contains("responsib")
                    || lower.contains("familiar")
                    || lower.contains("proficient")) {
                out.add(cut(line, 80));
            }
            if (out.size() >= limit) {
                break;
            }
        }
        return out;
    }

    private String toBulletList(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "- [Map JD requirements into evidence-backed skills]";
        }
        return lines.stream().map(line -> "- " + line).reduce((a, b) -> a + "\n" + b).orElse("");
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
