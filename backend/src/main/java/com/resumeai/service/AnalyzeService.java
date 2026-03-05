package com.resumeai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.dto.AnalyzeResponse;
import com.resumeai.dto.OptimizedBlock;
import com.resumeai.model.AiConfig;
import com.resumeai.model.AnalysisJob;
import com.resumeai.model.AnalysisRecord;
import com.resumeai.model.UserAccount;
import com.resumeai.repository.AnalysisRecordRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AnalyzeService {
    /*
     * 分析服务总览:
     * 1. 负责简历/JD文本提取与基础校验
     * 2. 使用本地规则计算 ATS 分数与关键词覆盖率
     * 3. 尝试调用大模型生成优化建议，失败则本地兜底
     * 4. 合并题库面试题并持久化分析记录
     */
    public static final int MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final Pattern EN_TOKEN = Pattern.compile("[a-zA-Z][a-zA-Z0-9_+.#-]{1,30}");
    private static final Pattern ZH_TOKEN = Pattern.compile("[\\u4e00-\\u9fff]{2,10}");
    private static final Pattern HAS_NUMBER = Pattern.compile(".*(\\d+|\\d+%|percent|ms|s|x|\\u500d).*", Pattern.CASE_INSENSITIVE);

    private static final Set<String> EN_STOPWORDS = Set.of(
            "the", "and", "for", "with", "from", "that", "this", "your", "you", "our", "are", "was",
            "were", "have", "has", "had", "will", "can", "able", "using", "used", "within", "across",
            "job", "role", "team", "work", "years", "year", "experience", "requirements", "responsibilities",
            "need", "must", "plus", "good", "strong", "ability", "skills", "skill"
    );

    private static final Set<String> ZH_STOPWORDS = Set.of(
            "\u6211\u4EEC", "\u4F60\u4EEC", "\u8D1F\u8D23", "\u80FD\u591F", "\u76F8\u5173", "\u4EE5\u53CA",
            "\u4EE5\u4E0A", "\u5177\u6709", "\u4F18\u5148", "\u5DE5\u4F5C", "\u7ECF\u9A8C", "\u80FD\u529B",
            "\u719F\u6089", "\u53C2\u4E0E", "\u5C97\u4F4D", "\u8981\u6C42", "\u804C\u4F4D", "\u516C\u53F8",
            "\u56E2\u961F", "\u5B8C\u6210", "\u5FC5\u987B", "\u9700\u8981", "\u638C\u63E1"
    );

    private static final Set<String> ACTION_HINTS = Set.of(
            "designed", "built", "delivered", "improved", "optimized", "implemented", "led", "scaled", "automated",
            "reduced", "increased", "achieved", "created", "launched", "debugged", "analyzed", "refactored",
            "\u8BBE\u8BA1", "\u5F00\u53D1", "\u4F18\u5316", "\u63D0\u5347", "\u843D\u5730", "\u63A8\u52A8",
            "\u5B9E\u73B0", "\u642D\u5EFA", "\u6539\u8FDB", "\u91CD\u6784", "\u6CBB\u7406", "\u4EA4\u4ED8"
    );

    private static final Set<String> REQUIREMENT_CUES = Set.of(
            "must", "required", "requirement", "need", "proficient", "strong", "hands-on",
            "\u719F\u6089", "\u638C\u63E1", "\u8981\u6C42", "\u5FC5\u987B", "\u5177\u5907"
    );

    private static final Map<String, String> ALIAS_TO_CANONICAL = buildAliasMap();

    private final AnalysisRecordRepository analysisRecordRepository;
    private final ConfigService configService;
    private final InterviewQuestionKbService interviewQuestionKbService;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;
    private final String ocrLang;
    private final String ocrDatapath;

    public AnalyzeService(
            AnalysisRecordRepository analysisRecordRepository,
            ConfigService configService,
            InterviewQuestionKbService interviewQuestionKbService,
            ObjectMapper objectMapper,
            WebClient.Builder webClientBuilder,
            @Value("${app.ocr.lang:chi_sim+eng}") String ocrLang,
            @Value("${app.ocr.datapath:}") String ocrDatapath) {
        this.analysisRecordRepository = analysisRecordRepository;
        this.configService = configService;
        this.interviewQuestionKbService = interviewQuestionKbService;
        this.objectMapper = objectMapper;
        this.webClientBuilder = webClientBuilder;
        this.ocrLang = clean(ocrLang).isEmpty() ? "chi_sim+eng" : clean(ocrLang);
        this.ocrDatapath = clean(ocrDatapath);
    }

    // 提交前校验: 文件必须有效，且 JD 文本或岗位信息至少有一项可用于分析
    public void validateSubmission(MultipartFile file, String jdText, String targetRole) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("file is too large");
        }
        String cleanRole = clean(targetRole);
        String cleanJd = clean(jdText);
        if (cleanJd.length() < 20 && cleanRole.length() < 2) {
            throw new IllegalArgumentException("jd_text should have at least 20 characters");
        }
    }

    // JD 获取优先级: 手工输入文本 > 图片 OCR（用于招聘网站不可复制文本的场景）
    public String resolveJdText(String jdText, MultipartFile jdImage) {
        String cleanJd = clean(jdText);
        if (cleanJd.length() >= 20) {
            return cleanJd;
        }
        if (jdImage == null || jdImage.isEmpty()) {
            return cleanJd;
        }
        if (jdImage.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("jd image is too large");
        }
        String filename = clean(jdImage.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!isImageFilename(filename)) {
            throw new IllegalArgumentException("jd image format not supported");
        }
        try {
            String ocrText = normalizeText(extractImageText(jdImage.getBytes()));
            if (ocrText.length() < 20) {
                throw new IllegalArgumentException("jd image text is too short");
            }
            return ocrText;
        } catch (IllegalArgumentException ex) {
            if ("image decode failed".equalsIgnoreCase(clean(ex.getMessage()))
                    || "image text is empty".equalsIgnoreCase(clean(ex.getMessage()))) {
                throw new IllegalArgumentException("failed to read jd image");
            }
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("failed to read jd image");
        }
    }

    // 队列消费入口: 读取落盘文件后，复用统一分析主流程
    public AnalyzeResponse processQueuedJob(AnalysisJob job, UserAccount user) {
        try {
            byte[] data = Files.readAllBytes(Path.of(job.getFilePath()));
            return analyzeInternal(data, job.getFilename(), job.getJdText(), job.getTargetRole(), user);
        } catch (IOException ex) {
            throw new IllegalArgumentException("failed to read queued resume file");
        }
    }

    // 统一分析主链路: 解析简历 -> ATS 报告 -> 模型/规则建议 -> 题库问题 -> 保存记录
    private AnalyzeResponse analyzeInternal(byte[] fileData, String filename, String jdText, String targetRole, UserAccount user) {
        String cleanRole = clean(targetRole);
        String cleanJd = clean(jdText);
        if (cleanJd.length() < 20) {
            cleanJd = buildRoleTemplate(cleanRole.isEmpty() ? "General Engineer" : cleanRole);
        }

        String resumeText = normalizeText(extractResumeText(fileData, filename));
        if (resumeText.length() < 30) {
            throw new IllegalArgumentException("resume text is too short");
        }

        AtsReport ats = buildAtsReport(resumeText, cleanJd, cleanRole);
        OptimizedBlock optimized = requestLlmOptimized(resumeText, cleanJd, cleanRole, ats);
        boolean modelUsed = optimized != null;
        if (!modelUsed) {
            optimized = fallbackOptimized(resumeText, cleanJd, cleanRole, ats);
        }
        List<String> kbQuestions = interviewQuestionKbService.retrieveQuestions(cleanRole, cleanJd, ats.missingKeywords, 5);
        optimized.setInterviewQuestions(mergeInterviewQuestions(kbQuestions, optimized.getInterviewQuestions(), 10));

        AnalyzeResponse response = new AnalyzeResponse();
        response.setScore(ats.score);
        response.setCoverage(ats.coverage);
        response.setMatchedKeywords(ats.matchedKeywords);
        response.setMissingKeywords(ats.missingKeywords);
        response.setModelUsed(modelUsed);
        response.setOptimized(optimized);
        response.setOptimizedMarkdown(toMarkdown(response));

        AnalysisRecord record = new AnalysisRecord();
        record.setCreatedAt(Instant.now());
        record.setFilename(filename);
        record.setUserId(user.getId());
        record.setUsername(user.getUsername());
        record.setTargetRole(cleanRole);
        record.setJdText(cleanJd);
        record.setResumeText(resumeText);
        record.setScore(ats.score);
        record.setCoverage(ats.coverage);
        record.setOptimizedSummary(cut(optimized.getSummary(), 1000));
        record.setModelUsed(modelUsed);
        try {
            record.setResultJson(objectMapper.writeValueAsString(response));
        } catch (Exception ignore) {
            record.setResultJson("{}");
        }
        record = analysisRecordRepository.save(record);
        response.setAnalysisId(record.getId());
        return response;
    }

    // ATS 报告生成: 关键词权重匹配 + 结构完整度 + 动作词强度 + 量化表达强度
    private AtsReport buildAtsReport(String resumeText, String jdText, String targetRole) {
        List<String> resumeTokens = tokenize(resumeText);
        Map<String, Integer> resumeFreq = frequency(resumeTokens);

        Map<String, Double> jdWeighted = buildJdWeights(jdText, targetRole);
        List<String> jdKeywords = jdWeighted.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(40)
                .map(Map.Entry::getKey)
                .toList();

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        double totalWeight = 0D;
        double matchedWeight = 0D;

        for (String key : jdKeywords) {
            double weight = jdWeighted.getOrDefault(key, 1D);
            totalWeight += weight;
            if (resumeFreq.containsKey(key)) {
                matched.add(key);
                matchedWeight += weight;
            } else {
                missing.add(key);
            }
        }

        double coverage = totalWeight <= 0 ? 0D : round2(matchedWeight / totalWeight * 100D);
        double sectionScore = sectionCompleteness(resumeText);
        double actionScore = actionStrength(resumeTokens);
        double quantScore = quantificationStrength(resumeText);
        double score = round2(Math.min(100D, coverage * 0.72 + sectionScore * 15 + actionScore * 8 + quantScore * 5));

        AtsReport report = new AtsReport();
        report.score = score;
        report.coverage = coverage;
        report.matchedKeywords = matched;
        report.missingKeywords = missing;
        report.resumeTopKeywords = topFromFreq(resumeFreq, 20);
        return report;
    }

    private Map<String, Double> buildJdWeights(String jdText, String targetRole) {
        Map<String, Double> weights = new HashMap<>();
        List<String> roleTokens = tokenize(targetRole);

        String[] lines = jdText.split("\\n+");
        for (String rawLine : lines) {
            String line = clean(rawLine);
            if (line.isEmpty()) continue;
            List<String> lineTokens = tokenize(line);
            boolean requirementLine = containsRequirementCue(line);
            for (String token : lineTokens) {
                double w = 1.0;
                if (requirementLine) w += 0.7;
                if (roleTokens.contains(token)) w += 0.5;
                weights.merge(token, w, Double::sum);
            }
        }
        return weights;
    }

    private boolean containsRequirementCue(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        for (String cue : REQUIREMENT_CUES) {
            if (lower.contains(cue.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        List<String> tokens = new ArrayList<>();

        Matcher en = EN_TOKEN.matcher(text.toLowerCase(Locale.ROOT));
        while (en.find()) {
            String tk = normalizeToken(en.group());
            if (tk.isEmpty() || tk.length() < 2 || EN_STOPWORDS.contains(tk)) continue;
            tokens.add(tk);
        }

        Matcher zh = ZH_TOKEN.matcher(text);
        while (zh.find()) {
            String tk = normalizeToken(zh.group());
            if (tk.isEmpty() || ZH_STOPWORDS.contains(tk)) continue;
            tokens.add(tk);
        }
        return tokens;
    }

    private String normalizeToken(String token) {
        String base = clean(token).toLowerCase(Locale.ROOT);
        if (base.isEmpty()) return "";
        base = base.replaceAll("^[^a-z0-9\\u4e00-\\u9fff]+|[^a-z0-9\\u4e00-\\u9fff]+$", "");
        if (base.isEmpty()) return "";
        return ALIAS_TO_CANONICAL.getOrDefault(base, base);
    }

    private Map<String, Integer> frequency(List<String> tokens) {
        Map<String, Integer> freq = new HashMap<>();
        for (String tk : tokens) freq.merge(tk, 1, Integer::sum);
        return freq;
    }

    private List<String> topFromFreq(Map<String, Integer> freq, int limit) {
        return freq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    private double sectionCompleteness(String resumeText) {
        String text = resumeText.toLowerCase(Locale.ROOT);
        int score = 0;
        if (resumeText.contains("@") || resumeText.matches(".*1\\d{10}.*")) score++;
        if (containsAny(text, "education", "school", "college", "university", "\u5B66\u5386", "\u6559\u80B2")) score++;
        if (containsAny(text, "experience", "employment", "worked", "\u7ECF\u5386", "\u5DE5\u4F5C")) score++;
        if (containsAny(text, "project", "projects", "\u9879\u76EE")) score++;
        if (containsAny(text, "skill", "skills", "stack", "\u6280\u80FD", "\u6280\u672F")) score++;
        return score / 5.0;
    }

    private double actionStrength(List<String> resumeTokens) {
        Set<String> tokenSet = new LinkedHashSet<>(resumeTokens);
        long hits = ACTION_HINTS.stream().map(this::normalizeToken).filter(tokenSet::contains).count();
        return Math.min(1.0, hits / 8.0);
    }

    private double quantificationStrength(String resumeText) {
        String[] lines = resumeText.split("\\n+");
        int valid = 0;
        int hit = 0;
        for (String raw : lines) {
            String line = clean(raw);
            if (line.length() < 12) continue;
            valid++;
            if (HAS_NUMBER.matcher(line).matches()) hit++;
        }
        if (valid == 0) return 0;
        return Math.min(1.0, (double) hit / Math.max(4, valid));
    }

    private boolean containsAny(String text, String... keys) {
        for (String key : keys) {
            if (text.contains(key.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    // 调用大模型生成优化建议; 返回 null 表示调用失败，交给本地兜底策略
    private OptimizedBlock requestLlmOptimized(String resumeText, String jdText, String targetRole, AtsReport ats) {
        AiConfig cfg = configService.getEntity();
        String apiKey = clean(cfg.getApiKey());
        String model = clean(cfg.getModel());
        if (apiKey.isEmpty() || model.isEmpty()) return null;

        String prompt = buildPrompt(resumeText, jdText, targetRole, ats);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0.2);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", "You are an ATS and resume coach. Return strict JSON only."),
                Map.of("role", "user", "content", prompt)
        ));

        try {
            WebClient client = webClientBuilder
                    .baseUrl(normalizeBaseUrl(cfg.getBaseUrl()))
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .build();

            JsonNode resp = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(45));
            if (resp == null) return null;
            String content = resp.path("choices").path(0).path("message").path("content").asText("");
            if (content.isEmpty()) return null;
            return parseOptimizedJson(content);
        } catch (Exception ex) {
            return null;
        }
    }

    private String buildPrompt(String resumeText, String jdText, String targetRole, AtsReport ats) {
        return """
                Analyze the resume against the job description.
                Return JSON only with fields:
                - summary: string
                - rewritten_experience: string[]
                - skills_recommendations: string[]
                - interview_questions: string[]

                Constraints:
                - 4-6 items for each array.
                - interview_questions must be specific to resume gaps and role expectations.
                - prefer STAR-style phrasing for rewritten_experience.

                Context:
                target_role: %s
                ats_score: %.2f
                ats_coverage: %.2f
                matched_keywords: %s
                missing_keywords: %s
                resume_top_keywords: %s

                Resume:
                %s

                Job Description:
                %s
                """.formatted(
                targetRole.isBlank() ? "unknown" : targetRole,
                ats.score,
                ats.coverage,
                ats.matchedKeywords,
                ats.missingKeywords,
                ats.resumeTopKeywords,
                cut(resumeText, 5000),
                cut(jdText, 4000)
        );
    }

    private OptimizedBlock parseOptimizedJson(String content) {
        try {
            String trimmed = content.trim();
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) trimmed = trimmed.substring(start, end + 1);
            JsonNode root = objectMapper.readTree(trimmed);

            OptimizedBlock block = new OptimizedBlock();
            block.setSummary(root.path("summary").asText("Improve role alignment and measurable achievements."));
            block.setRewrittenExperience(readArray(root.path("rewritten_experience")));
            block.setSkillsRecommendations(readArray(root.path("skills_recommendations")));
            block.setInterviewQuestions(readArray(root.path("interview_questions")));
            if (block.getRewrittenExperience().isEmpty()) return null;
            if (block.getInterviewQuestions().isEmpty()) return null;
            return block;
        } catch (Exception ex) {
            return null;
        }
    }

    private List<String> readArray(JsonNode node) {
        if (!node.isArray()) return new ArrayList<>();
        List<String> out = new ArrayList<>();
        for (JsonNode item : node) {
            String text = clean(item.asText(""));
            if (!text.isEmpty()) out.add(text);
        }
        return out;
    }

    // 本地兜底建议: 保证模型不可用时依然能输出可读、可执行的优化结果
    private OptimizedBlock fallbackOptimized(String resumeText, String jdText, String targetRole, AtsReport ats) {
        List<String> lines = Arrays.stream(resumeText.split("\\n+"))
                .map(String::trim)
                .filter(v -> v.length() >= 12)
                .limit(8)
                .toList();

        List<String> rewritten = new ArrayList<>();
        for (String line : lines.stream().limit(5).toList()) {
            rewritten.add(line + " | add action, architecture choice, and measurable impact.");
        }
        if (rewritten.isEmpty()) {
            rewritten.add("Describe one core project in STAR format and quantify outcome.");
        }

        List<String> interview = new ArrayList<>();
        for (String miss : ats.missingKeywords.stream().limit(4).toList()) {
            interview.add("How have you handled " + miss + " in real projects? Give one concrete example.");
        }
        interview.add("Walk through one project end-to-end: design, tradeoffs, and metrics.");
        interview.add("How do you validate reliability and performance after release?");

        OptimizedBlock block = new OptimizedBlock();
        block.setSummary("ATS score is " + ats.score + ". Improve missing keyword coverage and quantified delivery impact.");
        block.setRewrittenExperience(rewritten);
        block.setSkillsRecommendations(List.of(
                "Map each JD requirement to at least one proven project bullet.",
                "Group skills into Core, Framework, Database, and Observability.",
                "Remove tools without project evidence; keep evidence-backed skills only.",
                "Add one reliability and one performance tuning achievement with metrics."
        ));
        block.setInterviewQuestions(interview);
        return block;
    }

    private List<String> mergeInterviewQuestions(List<String> kbQuestions, List<String> generatedQuestions, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 12));
        List<String> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        if (kbQuestions != null) {
            for (String q : kbQuestions) {
                appendQuestion(out, seen, "【题库】" + clean(q), safeLimit);
            }
        }
        if (generatedQuestions != null) {
            for (String q : generatedQuestions) {
                appendQuestion(out, seen, clean(q), safeLimit);
            }
        }
        return out;
    }

    private void appendQuestion(List<String> out, Set<String> seen, String question, int limit) {
        if (out.size() >= limit) {
            return;
        }
        String text = clean(question);
        if (text.isEmpty()) {
            return;
        }
        String dedupKey = text
                .replace("【题库】", "")
                .replaceAll("[\\s\\p{Punct}？?]+", "")
                .toLowerCase(Locale.ROOT);
        if (dedupKey.length() < 4 || seen.contains(dedupKey)) {
            return;
        }
        seen.add(dedupKey);
        out.add(text);
    }

    // 将结构化结果转为 Markdown，便于历史记录展示与后续导出
    private String toMarkdown(AnalyzeResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Resume Optimization Report\n\n");
        sb.append("- ATS Score: ").append(response.getScore()).append("\n");
        sb.append("- Keyword Coverage: ").append(response.getCoverage()).append("%\n\n");
        sb.append("## Matched Keywords\n");
        for (String k : response.getMatchedKeywords()) sb.append("- ").append(k).append("\n");
        sb.append("\n## Missing Keywords\n");
        for (String k : response.getMissingKeywords()) sb.append("- ").append(k).append("\n");

        OptimizedBlock o = response.getOptimized();
        sb.append("\n## Summary\n").append(o.getSummary()).append("\n\n");
        sb.append("## Rewritten Experience\n");
        for (String line : o.getRewrittenExperience()) sb.append("- ").append(line).append("\n");
        sb.append("\n## Skills Recommendations\n");
        for (String line : o.getSkillsRecommendations()) sb.append("- ").append(line).append("\n");
        sb.append("\n## Interview Questions\n");
        for (String line : o.getInterviewQuestions()) sb.append("- ").append(line).append("\n");
        return sb.toString();
    }

    // 按文件后缀选择解析器: PDF / DOCX / 图片 OCR / 纯文本
    private String extractResumeText(byte[] data, String filename) {
        String lowerName = clean(filename).toLowerCase(Locale.ROOT);
        try {
            if (lowerName.endsWith(".pdf")) {
                return extractPdf(data);
            }
            if (lowerName.endsWith(".docx")) {
                return extractDocx(data);
            }
            if (isImageFilename(lowerName)) {
                return extractImageText(data);
            }
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalArgumentException("failed to read resume file");
        }
    }

    private String extractPdf(byte[] data) throws IOException {
        try (PDDocument document = Loader.loadPDF(data)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocx(byte[] data) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(data))) {
            List<String> lines = new ArrayList<>();
            for (XWPFParagraph p : doc.getParagraphs()) {
                String t = clean(p.getText());
                if (!t.isEmpty()) lines.add(t);
            }
            return String.join("\n", lines);
        }
    }

    private String extractImageText(byte[] data) throws IOException, TesseractException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
        if (image == null) {
            throw new IllegalArgumentException("image decode failed");
        }
        ITesseract tesseract = buildTesseract();
        String text = tesseract.doOCR(image);
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("image text is empty");
        }
        return text;
    }

    private ITesseract buildTesseract() {
        Tesseract tesseract = new Tesseract();
        if (!ocrDatapath.isBlank()) {
            tesseract.setDatapath(ocrDatapath);
        }
        tesseract.setLanguage(ocrLang);
        tesseract.setPageSegMode(6);
        return tesseract;
    }

    private boolean isImageFilename(String filename) {
        return filename.endsWith(".png")
                || filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")
                || filename.endsWith(".bmp")
                || filename.endsWith(".webp")
                || filename.endsWith(".tif")
                || filename.endsWith(".tiff");
    }

    private String normalizeText(String text) {
        String normalized = clean(text).replace("\r", "\n");
        return normalized.replaceAll("\\n{3,}", "\n\n");
    }

    private String buildRoleTemplate(String role) {
        return """
                Role: %s
                Responsibilities:
                1. Build and maintain backend services and APIs.
                2. Improve service reliability, performance, and observability.
                3. Own delivery quality and cross-team collaboration.
                Requirements:
                - Solid engineering fundamentals and communication.
                - Experience with service architecture and database design.
                - Ability to deliver measurable business outcomes.
                """.formatted(role);
    }

    private String normalizeBaseUrl(String baseUrl) {
        String url = clean(baseUrl);
        if (url.isEmpty()) {
            return "https://api.openai.com/v1";
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.matches("^https?://[^/]+$")) {
            return url + "/v1";
        }
        return url;
    }

    private String cut(String text, int max) {
        String value = clean(text);
        if (value.length() <= max) return value;
        return value.substring(0, max);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    private static Map<String, String> buildAliasMap() {
        List<List<String>> groups = List.of(
                List.of("java", "jdk", "jvm"),
                List.of("spring", "springboot", "spring-boot"),
                List.of("mysql", "sql", "mysql8"),
                List.of("redis", "cache"),
                List.of("kafka", "mq", "messagequeue"),
                List.of("microservice", "microservices", "\u5FAE\u670D\u52A1"),
                List.of("docker", "container"),
                List.of("kubernetes", "k8s"),
                List.of("api", "rest", "restful"),
                List.of("performance", "perf", "tuning", "\u4F18\u5316"),
                List.of("reliability", "stability", "availability", "\u7A33\u5B9A\u6027")
        );
        Map<String, String> map = new HashMap<>();
        for (List<String> group : groups) {
            String canonical = group.get(0);
            for (String alias : group) {
                map.put(alias.toLowerCase(Locale.ROOT), canonical);
            }
        }
        return map;
    }

    private static class AtsReport {
        private double score;
        private double coverage;
        private List<String> matchedKeywords = new ArrayList<>();
        private List<String> missingKeywords = new ArrayList<>();
        private List<String> resumeTopKeywords = new ArrayList<>();
    }
}
