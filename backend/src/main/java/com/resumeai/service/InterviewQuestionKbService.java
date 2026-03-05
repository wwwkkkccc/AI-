package com.resumeai.service;

import com.resumeai.dto.InterviewKbDocItem;
import com.resumeai.dto.InterviewKbDocsResponse;
import com.resumeai.model.InterviewKbDoc;
import com.resumeai.model.InterviewKbItem;
import com.resumeai.model.UserAccount;
import com.resumeai.repository.InterviewKbDocRepository;
import com.resumeai.repository.InterviewKbItemRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 面试题库核心服务。
 * 职责：
 * 1) 上传/导入面试题文档（支持 PDF、DOCX、TXT、MD、图片 OCR）
 * 2) 从文本中解析、规范化面试题并持久化
 * 3) 分页查询/删除题库文档
 * 4) 基于关键词匹配从题库中检索与目标岗位相关的面试题
 */
@Service
public class InterviewQuestionKbService {
    // 单文件最大大小（12MB）
    private static final int MAX_FILE_SIZE = 12 * 1024 * 1024;
    // 单文档最大题目数
    private static final int MAX_QUESTIONS_PER_DOC = 1500;
    // 题目最小长度
    private static final int MIN_QUESTION_LENGTH = 6;
    // 题目最大长度
    private static final int MAX_QUESTION_LENGTH = 260;

    // 英文 token 匹配正则
    private static final Pattern EN_TOKEN = Pattern.compile("[a-zA-Z][a-zA-Z0-9_+.#-]{1,30}");
    // 中文 token 匹配正则
    private static final Pattern ZH_TOKEN = Pattern.compile("[\\u4e00-\\u9fff]{2,10}");
    // 去除行首序号前缀的正则
    private static final Pattern PREFIX_NUMBER = Pattern.compile("^[\\s\\-•·●◆◇\\d一二三四五六七八九十①②③④⑤⑥⑦⑧⑨⑩()（）【】\\[\\].、,:：]+");
    // 按句拆分的正则
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("[。！？!?;；\\n]+");
    // 英文停用词
    private static final Set<String> EN_STOPWORDS = Set.of(
            "the", "and", "for", "with", "that", "this", "from", "your", "our", "into", "about", "what", "when"
    );
    // 中文停用词
    private static final Set<String> ZH_STOPWORDS = Set.of(
            "我们", "你们", "这个", "那个", "一个", "请问", "如何", "什么", "为什么", "怎么", "哪些", "以及"
    );
    // 面试题线索词（用于判断句子是否为面试题）
    private static final Set<String> QUESTION_CUES = Set.of(
            "what", "why", "how", "when", "difference", "compare", "explain", "describe", "design", "tradeoff",
            "什么", "为什么", "如何", "怎么", "区别", "原理", "流程", "场景", "设计", "优化", "排查", "介绍", "解释"
    );
    // 面试题起始词
    private static final Set<String> QUESTION_STARTERS = Set.of(
            "请", "如何", "为什么", "什么是", "谈谈", "简述", "说明",
            "what", "why", "how", "explain", "describe", "compare"
    );

    private final InterviewKbDocRepository docRepository;
    private final InterviewKbItemRepository itemRepository;
    private final String ocrLang;
    private final String ocrDatapath;

    public InterviewQuestionKbService(
            InterviewKbDocRepository docRepository,
            InterviewKbItemRepository itemRepository,
            @Value("${app.ocr.lang:chi_sim+eng}") String ocrLang,
            @Value("${app.ocr.datapath:/usr/share/tesseract-ocr/5/tessdata}") String ocrDatapath) {
        this.docRepository = docRepository;
        this.itemRepository = itemRepository;
        this.ocrLang = clean(ocrLang).isEmpty() ? "chi_sim+eng" : clean(ocrLang);
        this.ocrDatapath = clean(ocrDatapath);
    }

    /**
     * 上传面试题文档并导入题库。
     * 支持 PDF、DOCX、TXT、MD 和图片格式（OCR 识别）。
     */
    @Transactional
    public InterviewKbDocItem uploadDoc(MultipartFile file, String title, UserAccount admin) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("question doc file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("question doc file is too large");
        }

        String filename = safeFilename(file.getOriginalFilename());
        String text;
        try {
            text = normalizeText(extractFileText(file.getBytes(), filename));
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("failed to read question doc file");
        }
        return importText(text, title, filename, admin);
    }

    /** 从纯文本中解析面试题并导入题库 */
    @Transactional
    public InterviewKbDocItem importText(String text, String title, String filename, UserAccount admin) {
        List<String> questions = parseQuestions(text);
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("no interview questions found in document");
        }
        return saveDoc(questions, title, filename, admin);
    }

    /** 从已提取的题目列表导入题库（由爬虫/LLM 服务调用） */
    @Transactional
    public InterviewKbDocItem importQuestions(List<String> questions, String title, String filename, UserAccount admin) {
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("no interview questions found in crawled pages");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String raw : questions) {
            addQuestionFromLine(normalizeQuestion(raw), normalized);
            if (normalized.size() >= MAX_QUESTIONS_PER_DOC) {
                break;
            }
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("no interview questions found in crawled pages");
        }
        return saveDoc(new ArrayList<>(normalized), title, filename, admin);
    }

    /** 分页查询题库文档列表，按 ID 倒序 */
    @Transactional(readOnly = true)
    public InterviewKbDocsResponse listDocs(int page, int size) {
        Pageable pageable = normalizePage(page, size);
        Page<InterviewKbDoc> docs = docRepository.findAllByOrderByIdDesc(pageable);
        InterviewKbDocsResponse response = new InterviewKbDocsResponse();
        response.setItems(docs.getContent().stream().map(this::toDocItem).toList());
        response.setTotal(docs.getTotalElements());
        response.setPage(pageable.getPageNumber());
        response.setSize(pageable.getPageSize());
        return response;
    }

    /** 删除指定题库文档及其下所有面试题 */
    @Transactional
    public void deleteDoc(Long docId) {
        InterviewKbDoc doc = docRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("question doc not found"));
        itemRepository.deleteByDocId(doc.getId());
        docRepository.delete(doc);
    }

    /**
     * 根据目标岗位、JD 文本和缺失关键词，从题库中检索最相关的面试题。
     * 使用关键词重叠度 + 文本包含度进行评分排序。
     */
    @Transactional(readOnly = true)
    public List<String> retrieveQuestions(String targetRole, String jdText, List<String> missingKeywords, int limit) {
        int safeLimit = Math.max(1, Math.min(12, limit));
        List<InterviewKbItem> items = itemRepository.findTop3000ByOrderByIdDesc();
        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        String missingText = missingKeywords == null ? "" : String.join(" ", missingKeywords);
        String queryText = String.join("\n", List.of(clean(targetRole), clean(jdText), missingText));
        Set<String> queryTokens = new LinkedHashSet<>(extractKeywordTokens(queryText, 80));
        if (queryTokens.isEmpty()) {
            return items.stream()
                    .map(InterviewKbItem::getQuestionText)
                    .map(this::clean)
                    .filter(v -> !v.isEmpty())
                    .distinct()
                    .limit(safeLimit)
                    .toList();
        }

        List<ScoredQuestion> scored = new ArrayList<>();
        for (InterviewKbItem item : items) {
            String question = clean(item.getQuestionText());
            if (question.isEmpty()) {
                continue;
            }
            Set<String> itemTokens = parseKeywords(item.getKeywords());
            int overlap = 0;
            for (String token : queryTokens) {
                if (itemTokens.contains(token)) {
                    overlap++;
                }
            }
            int containsBoost = 0;
            String lower = question.toLowerCase(Locale.ROOT);
            for (String token : queryTokens) {
                if (token.length() >= 3 && lower.contains(token)) {
                    containsBoost++;
                }
                if (containsBoost >= 5) {
                    break;
                }
            }
            double score = overlap * 3.0 + containsBoost;
            if (score <= 0) {
                continue;
            }
            scored.add(new ScoredQuestion(question, score, item.getId() == null ? 0L : item.getId()));
        }

        if (scored.isEmpty()) {
            return items.stream()
                    .map(InterviewKbItem::getQuestionText)
                    .map(this::clean)
                    .filter(v -> !v.isEmpty())
                    .distinct()
                    .limit(safeLimit)
                    .toList();
        }

        scored.sort((a, b) -> {
            int cmp = Double.compare(b.score, a.score);
            if (cmp != 0) {
                return cmp;
            }
            return Long.compare(b.id, a.id);
        });

        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (ScoredQuestion s : scored) {
            if (unique.size() >= safeLimit) {
                break;
            }
            unique.add(s.question);
        }
        return new ArrayList<>(unique);
    }

    /** 保存题库文档及其面试题到数据库 */
    private InterviewKbDocItem saveDoc(List<String> questions, String title, String filename, UserAccount admin) {
        String finalFilename = safeFilename(filename);
        String finalTitle = normalizeTitle(title, finalFilename);

        InterviewKbDoc doc = new InterviewKbDoc();
        doc.setTitle(finalTitle);
        doc.setFilename(finalFilename);
        doc.setUploadedById(admin == null ? null : admin.getId());
        doc.setUploadedBy(admin == null ? "unknown" : clean(admin.getUsername()));
        doc.setQuestionCount(Math.min(questions.size(), MAX_QUESTIONS_PER_DOC));
        doc.setCreatedAt(Instant.now());
        doc = docRepository.save(doc);

        List<InterviewKbItem> items = new ArrayList<>();
        Instant now = Instant.now();
        int count = 0;
        for (String question : questions) {
            if (count >= MAX_QUESTIONS_PER_DOC) {
                break;
            }
            String normalizedQuestion = normalizeQuestion(question);
            if (!looksLikeQuestion(normalizedQuestion)) {
                continue;
            }
            InterviewKbItem item = new InterviewKbItem();
            item.setDocId(doc.getId());
            item.setQuestionText(normalizedQuestion);
            item.setKeywords(String.join(",", extractKeywordTokens(normalizedQuestion, 24)));
            item.setCreatedAt(now);
            items.add(item);
            count++;
        }
        if (items.isEmpty()) {
            throw new IllegalArgumentException("no interview questions found in document");
        }
        itemRepository.saveAll(items);

        doc.setQuestionCount(items.size());
        doc = docRepository.save(doc);
        return toDocItem(doc);
    }

    /** 将文档实体转换为列表项 DTO */
    private InterviewKbDocItem toDocItem(InterviewKbDoc doc) {
        InterviewKbDocItem item = new InterviewKbDocItem();
        item.setId(doc.getId());
        item.setTitle(doc.getTitle());
        item.setFilename(doc.getFilename());
        item.setQuestionCount(doc.getQuestionCount());
        item.setUploadedById(doc.getUploadedById());
        item.setUploadedBy(doc.getUploadedBy());
        item.setCreatedAt(doc.getCreatedAt());
        return item;
    }

    /** 从文本中解析面试题：先按行拆分，不够则按句拆分补充 */
    private List<String> parseQuestions(String text) {
        if (clean(text).isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();

        String[] lines = normalizeText(text).split("\\n+");
        for (String raw : lines) {
            String line = normalizeQuestion(raw);
            if (line.isEmpty()) {
                continue;
            }
            addQuestionFromLine(line, out);
            if (out.size() >= MAX_QUESTIONS_PER_DOC) {
                break;
            }
        }

        if (out.isEmpty()) {
            for (String sentence : splitSentences(text)) {
                String q = normalizeQuestion(sentence);
                if (looksLikeQuestion(q)) {
                    out.add(q);
                }
                if (out.size() >= MAX_QUESTIONS_PER_DOC) {
                    break;
                }
            }
        }
        return new ArrayList<>(out);
    }

    /** 处理单行文本：过长或含句号则按句拆分，否则直接判断是否为面试题 */
    private void addQuestionFromLine(String line, Set<String> out) {
        if (line.isEmpty()) {
            return;
        }
        if (line.length() > MAX_QUESTION_LENGTH || line.contains("。") || line.contains("；") || line.contains(";")) {
            for (String sentence : splitSentences(line)) {
                String q = normalizeQuestion(sentence);
                if (looksLikeQuestion(q)) {
                    out.add(q);
                }
            }
            return;
        }
        if (looksLikeQuestion(line)) {
            out.add(line);
        }
    }

    /** 按句号/问号等标点拆分文本为句子列表 */
    private List<String> splitSentences(String text) {
        return Arrays.stream(SENTENCE_SPLIT.split(normalizeText(text)))
                .map(this::clean)
                .filter(v -> !v.isEmpty())
                .toList();
    }

    /** 判断文本是否像面试题：含问号、含线索词、以提问词开头、或以语气词结尾 */
    private boolean looksLikeQuestion(String text) {
        String q = clean(text);
        if (q.length() < MIN_QUESTION_LENGTH || q.length() > MAX_QUESTION_LENGTH) {
            return false;
        }
        if (q.contains("?") || q.contains("？")) {
            return true;
        }
        String lower = q.toLowerCase(Locale.ROOT);
        for (String cue : QUESTION_CUES) {
            if (lower.contains(cue)) {
                return true;
            }
        }
        for (String starter : QUESTION_STARTERS) {
            if (q.startsWith(starter) || lower.startsWith(starter)) {
                return true;
            }
        }
        return lower.endsWith("吗") || lower.endsWith("呢") || lower.endsWith("么");
    }

    /** 规范化面试题文本：去序号前缀、合并空白、去首尾标点、截断过长文本 */
    private String normalizeQuestion(String text) {
        String q = clean(text);
        if (q.isEmpty()) {
            return "";
        }
        q = PREFIX_NUMBER.matcher(q).replaceFirst("");
        q = q.replaceAll("\\s+", " ").trim();
        q = q.replaceAll("^[,，。;；:：]+|[,，。;；:：]+$", "").trim();
        if (q.length() > MAX_QUESTION_LENGTH) {
            q = q.substring(0, MAX_QUESTION_LENGTH).trim();
        }
        return q;
    }

    /** 根据文件扩展名提取文本内容 */
    private String extractFileText(byte[] data, String filename) throws IOException, TesseractException {
        String lower = clean(filename).toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return extractPdf(data);
        }
        if (lower.endsWith(".docx")) {
            return extractDocx(data);
        }
        if (lower.endsWith(".txt") || lower.endsWith(".md")) {
            return new String(data, StandardCharsets.UTF_8);
        }
        if (isImageFilename(lower)) {
            return extractImageText(data);
        }
        throw new IllegalArgumentException("question doc format not supported");
    }

    /** 从 PDF 文件中提取文本 */
    private String extractPdf(byte[] data) throws IOException {
        try (PDDocument document = Loader.loadPDF(data)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /** 从 DOCX 文件中提取段落文本 */
    private String extractDocx(byte[] data) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(data))) {
            List<String> lines = new ArrayList<>();
            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                String text = clean(paragraph.getText());
                if (!text.isEmpty()) {
                    lines.add(text);
                }
            }
            return String.join("\n", lines);
        }
    }

    /** 通过 OCR 从图片中提取文本 */
    private String extractImageText(byte[] data) throws IOException, TesseractException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
        if (image == null) {
            throw new IllegalArgumentException("failed to read question doc file");
        }
        ITesseract tesseract = buildTesseract();
        String text = tesseract.doOCR(image);
        if (clean(text).isEmpty()) {
            throw new IllegalArgumentException("no interview questions found in document");
        }
        return text;
    }

    /** 构建 Tesseract OCR 实例 */
    private ITesseract buildTesseract() {
        Tesseract tesseract = new Tesseract();
        if (!ocrDatapath.isBlank()) {
            tesseract.setDatapath(ocrDatapath);
        }
        tesseract.setLanguage(ocrLang);
        tesseract.setPageSegMode(6);
        return tesseract;
    }

    /** 判断文件名是否为图片格式 */
    private boolean isImageFilename(String filename) {
        return filename.endsWith(".png")
                || filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")
                || filename.endsWith(".bmp")
                || filename.endsWith(".webp")
                || filename.endsWith(".tif")
                || filename.endsWith(".tiff");
    }

    /** 从文本中提取关键词 token（中英文），用于题目检索匹配 */
    private List<String> extractKeywordTokens(String text, int limit) {
        if (clean(text).isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        Matcher en = EN_TOKEN.matcher(text.toLowerCase(Locale.ROOT));
        while (en.find() && out.size() < limit) {
            String token = normalizeToken(en.group());
            if (token.length() < 2 || EN_STOPWORDS.contains(token)) {
                continue;
            }
            out.add(token);
        }

        Matcher zh = ZH_TOKEN.matcher(text);
        while (zh.find() && out.size() < limit) {
            String token = normalizeToken(zh.group());
            if (token.length() < 2 || ZH_STOPWORDS.contains(token)) {
                continue;
            }
            out.add(token);
        }
        return new ArrayList<>(out);
    }

    /** 解析逗号分隔的关键词字符串为 Set */
    private Set<String> parseKeywords(String csv) {
        if (clean(csv).isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(csv.split(","))
                .map(this::normalizeToken)
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** 规范化 token：转小写并去除首尾非字母数字字符 */
    private String normalizeToken(String token) {
        String t = clean(token).toLowerCase(Locale.ROOT);
        if (t.isEmpty()) {
            return "";
        }
        return t.replaceAll("^[^a-z0-9\\u4e00-\\u9fff]+|[^a-z0-9\\u4e00-\\u9fff]+$", "");
    }

    /** 安全化文件名：去除非法字符、限制长度 */
    private String safeFilename(String filename) {
        String value = clean(filename);
        if (value.isEmpty()) {
            return "question_doc.txt";
        }
        value = value.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (value.length() > 180) {
            value = value.substring(value.length() - 180);
        }
        return value.toLowerCase(Locale.ROOT);
    }

    /** 规范化文档标题：为空时用文件名代替，超长则截断 */
    private String normalizeTitle(String title, String filename) {
        String finalTitle = clean(title);
        if (finalTitle.isEmpty()) {
            finalTitle = clean(filename);
        }
        if (finalTitle.length() > 255) {
            finalTitle = finalTitle.substring(0, 255);
        }
        return finalTitle;
    }

    /** 规范化分页参数，防止越界 */
    private Pageable normalizePage(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        return PageRequest.of(safePage, safeSize);
    }

    private String normalizeText(String text) {
        return clean(text)
                .replace("\u0000", "")
                .replace("\r", "\n")
                .replaceAll("\\n{3,}", "\n\n");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static class ScoredQuestion {
        private final String question;
        private final double score;
        private final long id;

        private ScoredQuestion(String question, double score, long id) {
            this.question = question;
            this.score = score;
            this.id = id;
        }
    }
}
