package com.resumeai.service;

import com.resumeai.dto.InterviewKbCrawlRequest;
import com.resumeai.dto.InterviewKbCrawlResponse;
import com.resumeai.dto.InterviewKbDocItem;
import com.resumeai.model.UserAccount;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

@Service
public class InterviewKbCrawlerService {
    /*
     * 爬虫服务职责:
     * 1) 从种子链接按深度/页数抓取网页
     * 2) 从正文与脚本中抽取候选面试题
     * 3) 识别私有登录页，并支持 Cookie/Authorization 定向抓取
     * 4) 将结果写入题库文档
     */
    private static final int DEFAULT_MAX_PAGES = 20;
    private static final int DEFAULT_MAX_DEPTH = 1;
    private static final int HARD_MAX_PAGES = 80;
    private static final int HARD_MAX_DEPTH = 3;
    private static final int FETCH_TIMEOUT_MS = 12000;
    private static final int MAX_BODY_SIZE = 2 * 1024 * 1024;
    private static final int MAX_ERRORS = 20;
    private static final int MAX_QUESTIONS_PER_PAGE = 180;
    private static final int MAX_SCRIPT_BLOCKS = 80;
    private static final int MAX_SCRIPT_CHARS = 120_000;

    private static final Pattern SENTENCE_SPLIT = Pattern.compile("[\\n\\r]+|[。！？!?；;]+");
    private static final Pattern PREFIX_NUMBER = Pattern.compile("^[\\s\\-\\d\\)\\(\\[\\]【】\\.、:：]+");
    private static final Pattern IPV4_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+\\.\\d+$");
    private static final Pattern UNICODE_ESCAPE = Pattern.compile("\\\\u([0-9a-fA-F]{4})");

    private static final Set<String> QUESTION_CUES = Set.of(
            "what", "why", "how", "when", "explain", "describe", "compare", "difference",
            "什么", "为什么", "如何", "怎么", "区别", "原理", "流程", "场景", "设计", "优化", "排查", "解释"
    );
    private static final Set<String> QUESTION_STARTERS = Set.of(
            "请", "如何", "为什么", "什么是", "谈谈", "简述", "说明",
            "what", "why", "how", "explain", "describe", "compare"
    );
    private static final Set<String> KNOWLEDGE_CUES = Set.of(
            "原理", "机制", "流程", "架构", "模型", "标准", "规范", "策略", "方法", "应用", "案例", "风险", "指标",
            "principle", "architecture", "mechanism", "workflow", "standard", "strategy", "method", "practice", "risk"
    );
    private static final Set<String> NOISE_CUES = Set.of(
            "登录", "注册", "首页", "联系我们", "隐私", "版权", "cookie", "privacy", "copyright", "terms"
    );
    private static final Set<String> LOGIN_CUES = Set.of(
            "login", "sign in", "passport", "登录", "扫码登录", "验证码", "feishu", "lark"
    );
    private static final Set<String> SKIP_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg", ".pdf", ".zip", ".rar", ".7z",
            ".tar", ".gz", ".mp3", ".mp4", ".avi", ".mov", ".apk", ".exe", ".dmg", ".iso", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx"
    );
    private static final DateTimeFormatter TITLE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm", Locale.ROOT).withZone(ZoneId.of("Asia/Shanghai"));

    private final InterviewQuestionKbService interviewQuestionKbService;

    public InterviewKbCrawlerService(InterviewQuestionKbService interviewQuestionKbService) {
        this.interviewQuestionKbService = interviewQuestionKbService;
    }

    // 爬取主流程: BFS 抓取 + 提取问题 + 导入题库 + 返回统计信息
    public InterviewKbCrawlResponse crawlAndImport(InterviewKbCrawlRequest req, UserAccount admin) {
        URI seedUri = parseSeedUrl(req.getSeedUrl());
        String seedUrl = normalizeUrl(seedUri.toString());
        String rootHost = normalizeHost(seedUri.getHost());
        int maxPages = normalizeMaxPages(req.getMaxPages());
        int maxDepth = normalizeMaxDepth(req.getMaxDepth());
        boolean sameDomainOnly = req.getSameDomainOnly() == null || req.getSameDomainOnly();
        FetchContext fetchContext = new FetchContext(
                clean(req.getAuthCookie()),
                clean(req.getReferer()),
                clean(req.getAuthHeader())
        );

        Deque<CrawlNode> queue = new ArrayDeque<>();
        Set<String> queued = new HashSet<>();
        Set<String> visited = new LinkedHashSet<>();
        List<String> errors = new ArrayList<>();
        LinkedHashSet<String> questions = new LinkedHashSet<>();
        boolean blockedByLogin = false;

        queue.offer(new CrawlNode(seedUrl, 0));
        queued.add(seedUrl);

        int pagesSucceeded = 0;
        int questionsExtracted = 0;

        while (!queue.isEmpty() && visited.size() < maxPages) {
            CrawlNode node = queue.poll();
            if (!visited.add(node.url)) {
                continue;
            }

            try {
                Document doc = fetchDocument(node.url, fetchContext);
                pagesSucceeded++;

                LinkedHashSet<String> pageQuestions = extractQuestions(doc);
                questionsExtracted += pageQuestions.size();
                questions.addAll(pageQuestions);

                if (node.depth < maxDepth) {
                    for (String next : extractNextLinks(doc, rootHost, sameDomainOnly)) {
                        if (queued.add(next)) {
                            queue.offer(new CrawlNode(next, node.depth + 1));
                        }
                    }
                }
            } catch (IllegalArgumentException ex) {
                String msg = clean(ex.getMessage());
                if ("private page requires login cookie".equals(msg)) {
                    blockedByLogin = true;
                }
                addError(errors, node.url + " -> " + cut(msg, 160));
            } catch (Exception ex) {
                addError(errors, node.url + " -> " + cut(clean(ex.getMessage()), 160));
            }
        }

        if (questions.isEmpty()) {
            if (blockedByLogin) {
                throw new IllegalArgumentException("private page requires login cookie");
            }
            throw new IllegalArgumentException("no interview questions found in crawled pages");
        }

        String filename = buildFilename(seedUri);
        String title = buildTitle(req, seedUri);
        InterviewKbDocItem doc = interviewQuestionKbService.importQuestions(new ArrayList<>(questions), title, filename, admin);

        InterviewKbCrawlResponse response = new InterviewKbCrawlResponse();
        response.setSeedUrl(seedUrl);
        response.setPagesVisited(visited.size());
        response.setPagesSucceeded(pagesSucceeded);
        response.setQuestionsExtracted(questionsExtracted);
        response.setQuestionsImported(doc.getQuestionCount() == null ? 0 : doc.getQuestionCount());
        response.setDoc(doc);
        response.setErrors(errors);
        return response;
    }

    // 页面拉取: 可选注入 Cookie / Referer / Authorization 以支持私有站点
    private Document fetchDocument(String url, FetchContext context) throws Exception {
        Connection connection = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; ResumeAiBot/1.0; +https://resume-ai.local)")
                .timeout(FETCH_TIMEOUT_MS)
                .maxBodySize(MAX_BODY_SIZE)
                .followRedirects(true);

        connection.header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        if (!context.referer.isEmpty()) {
            connection.referrer(context.referer);
        }
        if (!context.authHeader.isEmpty()) {
            connection.header("Authorization", context.authHeader);
        }
        if (!context.cookieHeader.isEmpty()) {
            connection.header("Cookie", context.cookieHeader);
            for (CookiePair pair : parseCookiePairs(context.cookieHeader)) {
                if (!pair.key.isEmpty() && !pair.value.isEmpty()) {
                    connection.cookie(pair.key, pair.value);
                }
            }
        }

        Document doc = connection.get();
        if (looksLikeLoginPage(doc) && context.authHeader.isEmpty() && context.cookieHeader.isEmpty()) {
            throw new IllegalArgumentException("private page requires login cookie");
        }
        return doc;
    }

    // 抽取策略: 先扫 script（处理前端渲染场景），再扫正文块元素
    private LinkedHashSet<String> extractQuestions(Document doc) {
        LinkedHashSet<String> out = new LinkedHashSet<>();

        extractQuestionsFromScript(doc, out);

        Document clone = doc.clone();
        clone.select("script,style,noscript,template,svg").remove();
        Elements blocks = clone.select("h1,h2,h3,h4,h5,h6,p,li,dt,dd,blockquote,td,th");

        int inspected = 0;
        for (Element block : blocks) {
            if (inspected >= 1600 || out.size() >= MAX_QUESTIONS_PER_PAGE) {
                break;
            }
            inspected++;
            collectQuestionsFromText(block.text(), out);
        }

        if (out.size() < 8) {
            collectQuestionsFromText(clone.text(), out);
        }
        return out;
    }

    // 从 script 文本里提取候选问题，兼容 JSON 字符串与 unicode 转义
    private void extractQuestionsFromScript(Document doc, Set<String> out) {
        int inspected = 0;
        for (Element script : doc.select("script")) {
            if (inspected >= MAX_SCRIPT_BLOCKS || out.size() >= MAX_QUESTIONS_PER_PAGE) {
                break;
            }
            inspected++;

            String data = clean(script.data());
            if (data.isEmpty()) {
                data = clean(script.html());
            }
            if (data.isEmpty()) {
                continue;
            }
            if (data.length() > MAX_SCRIPT_CHARS) {
                data = data.substring(0, MAX_SCRIPT_CHARS);
            }

            String decoded = decodeScriptText(data);
            if (decoded.length() < 20) {
                continue;
            }
            collectQuestionsFromText(decoded, out);
        }
    }

    // 从任意文本块中筛选“疑似问题”或“可转问题的知识句”
    private void collectQuestionsFromText(String text, Set<String> out) {
        String normalizedBlock = normalizeSentence(text);
        if (normalizedBlock.length() < 6 || normalizedBlock.length() > 12000) {
            return;
        }
        if (looksLikeNoise(normalizedBlock)) {
            return;
        }

        String[] segments = SENTENCE_SPLIT.split(normalizedBlock);
        for (String segment : segments) {
            if (out.size() >= MAX_QUESTIONS_PER_PAGE) {
                break;
            }
            String sentence = normalizeSentence(segment);
            if (sentence.length() < 6 || sentence.length() > 240) {
                continue;
            }
            if (looksLikeQuestion(sentence)) {
                out.add(ensureQuestionMark(sentence));
                continue;
            }
            if (looksLikeKnowledge(sentence)) {
                out.add(toQuestion(sentence));
            }
        }
    }

    // 链接发现: 标准化 URL、过滤非 http(s)/非允许域名/非文本资源
    private List<String> extractNextLinks(Document doc, String rootHost, boolean sameDomainOnly) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (Element anchor : doc.select("a[href]")) {
            String absUrl = clean(anchor.attr("abs:href"));
            if (absUrl.isEmpty()) {
                continue;
            }
            String normalized = normalizeUrl(absUrl);
            if (normalized.isEmpty()) {
                continue;
            }

            URI uri;
            try {
                uri = new URI(normalized);
            } catch (URISyntaxException ex) {
                continue;
            }
            String host = normalizeHost(uri.getHost());
            if (!isAllowedHost(host)) {
                continue;
            }
            if (sameDomainOnly && !rootHost.equals(host)) {
                continue;
            }
            if (hasSkippedExtension(uri.getPath())) {
                continue;
            }
            out.add(normalized);
        }
        return new ArrayList<>(out);
    }

    // 种子链接安全校验，防止抓取本地/内网地址
    private URI parseSeedUrl(String seedUrl) {
        String normalized = normalizeUrl(seedUrl);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("seed_url is required");
        }
        try {
            URI uri = new URI(normalized);
            String scheme = clean(uri.getScheme()).toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new IllegalArgumentException("seed_url must start with http or https");
            }
            String host = normalizeHost(uri.getHost());
            if (!isAllowedHost(host)) {
                throw new IllegalArgumentException("seed_url host is not allowed");
            }
            return uri;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("seed_url is invalid");
        }
    }

    private String normalizeUrl(String rawUrl) {
        String input = clean(rawUrl);
        if (input.isEmpty()) {
            return "";
        }
        try {
            URI uri = new URI(input);
            String scheme = clean(uri.getScheme()).toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                return "";
            }
            String host = normalizeHost(uri.getHost());
            if (host.isEmpty()) {
                return "";
            }

            String path = clean(uri.getRawPath());
            if (path.isEmpty()) {
                path = "/";
            }
            if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            URI normalized = new URI(
                    scheme,
                    uri.getUserInfo(),
                    host,
                    uri.getPort(),
                    path,
                    uri.getRawQuery(),
                    null
            );
            return normalized.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private boolean isAllowedHost(String host) {
        if (host.isEmpty()) {
            return false;
        }
        if ("localhost".equals(host) || host.endsWith(".local")) {
            return false;
        }

        if (IPV4_PATTERN.matcher(host).matches()) {
            String[] parts = host.split("\\.");
            if (parts.length != 4) {
                return false;
            }
            int[] ip = new int[4];
            for (int i = 0; i < 4; i++) {
                try {
                    ip[i] = Integer.parseInt(parts[i]);
                } catch (NumberFormatException ex) {
                    return false;
                }
                if (ip[i] < 0 || ip[i] > 255) {
                    return false;
                }
            }
            if (ip[0] == 10 || ip[0] == 127 || ip[0] == 0) {
                return false;
            }
            if (ip[0] == 169 && ip[1] == 254) {
                return false;
            }
            if (ip[0] == 192 && ip[1] == 168) {
                return false;
            }
            if (ip[0] == 172 && ip[1] >= 16 && ip[1] <= 31) {
                return false;
            }
            return true;
        }

        if (host.contains(":")) {
            String lower = host.toLowerCase(Locale.ROOT);
            if ("::1".equals(lower) || lower.startsWith("fe80:") || lower.startsWith("fc") || lower.startsWith("fd")) {
                return false;
            }
        }
        return true;
    }

    private boolean hasSkippedExtension(String path) {
        String value = clean(path).toLowerCase(Locale.ROOT);
        for (String ext : SKIP_EXTENSIONS) {
            if (value.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private boolean looksLikeQuestion(String text) {
        String value = clean(text);
        if (value.contains("?") || value.contains("？")) {
            return true;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        for (String cue : QUESTION_CUES) {
            if (lower.contains(cue)) {
                return true;
            }
        }
        for (String starter : QUESTION_STARTERS) {
            if (value.startsWith(starter) || lower.startsWith(starter)) {
                return true;
            }
        }
        return lower.endsWith("吗") || lower.endsWith("么") || lower.endsWith("呢");
    }

    private boolean looksLikeKnowledge(String text) {
        String value = clean(text);
        if (value.length() < 8 || value.length() > 120) {
            return false;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return false;
        }

        String lower = value.toLowerCase(Locale.ROOT);
        for (String noise : NOISE_CUES) {
            if (lower.contains(noise)) {
                return false;
            }
        }
        for (String cue : KNOWLEDGE_CUES) {
            if (lower.contains(cue)) {
                return true;
            }
        }
        return false;
    }

    private boolean looksLikeNoise(String text) {
        String lower = clean(text).toLowerCase(Locale.ROOT);
        for (String cue : NOISE_CUES) {
            if (lower.contains(cue)) {
                return true;
            }
        }
        return false;
    }

    // 登录页启发式判断: 关键字命中 + 页面文本体量较小
    private boolean looksLikeLoginPage(Document doc) {
        String title = clean(doc.title()).toLowerCase(Locale.ROOT);
        String text = clean(doc.text()).toLowerCase(Locale.ROOT);
        String combined = title + " " + text;
        int hits = 0;
        for (String cue : LOGIN_CUES) {
            if (combined.contains(cue)) {
                hits++;
            }
        }
        return hits >= 2 && text.length() < 8000;
    }

    private String toQuestion(String sentence) {
        String value = sentence.replaceAll("[。！？!?；;]+$", "").trim();
        if (containsChinese(value)) {
            return "请解释" + value + "，并给出一个实际应用场景？";
        }
        return "Explain " + value + " and provide one practical scenario?";
    }

    private String ensureQuestionMark(String sentence) {
        String value = sentence.replaceAll("[。！？!?；;]+$", "").trim();
        if (value.endsWith("?") || value.endsWith("？")) {
            return value;
        }
        if (containsChinese(value)) {
            return value + "？";
        }
        return value + "?";
    }

    private boolean containsChinese(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '\u4e00' && c <= '\u9fff') {
                return true;
            }
        }
        return false;
    }

    private String normalizeSentence(String sentence) {
        String value = clean(sentence);
        if (value.isEmpty()) {
            return "";
        }
        value = PREFIX_NUMBER.matcher(value).replaceFirst("");
        value = value.replaceAll("\\s+", " ");
        value = value.replaceAll("^[,，。！？!?:：;；]+|[,，。！？!?:：;；]+$", "");
        return value.trim();
    }

    // script 解码: 处理转义换行与 \\uXXXX，提升题目识别率
    private String decodeScriptText(String text) {
        String value = clean(text)
                .replace("\\n", " ")
                .replace("\\r", " ")
                .replace("\\t", " ")
                .replace("\\\\", "\\")
                .replace("\\\"", "\"")
                .replace("\\/", "/");

        Matcher matcher = UNICODE_ESCAPE.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            try {
                int code = Integer.parseInt(matcher.group(1), 16);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf((char) code)));
            } catch (Exception ex) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private List<CookiePair> parseCookiePairs(String rawCookie) {
        String value = clean(rawCookie);
        if (value.isEmpty()) {
            return List.of();
        }
        List<CookiePair> out = new ArrayList<>();
        String[] pairs = value.split(";");
        for (String pair : pairs) {
            String one = clean(pair);
            int idx = one.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = clean(one.substring(0, idx));
            String val = clean(one.substring(idx + 1));
            if (!key.isEmpty() && !val.isEmpty()) {
                out.add(new CookiePair(key, val));
            }
        }
        return out;
    }

    private int normalizeMaxPages(Integer maxPages) {
        int pages = maxPages == null ? DEFAULT_MAX_PAGES : maxPages;
        if (pages < 1) {
            pages = 1;
        }
        return Math.min(pages, HARD_MAX_PAGES);
    }

    private int normalizeMaxDepth(Integer maxDepth) {
        int depth = maxDepth == null ? DEFAULT_MAX_DEPTH : maxDepth;
        if (depth < 0) {
            depth = 0;
        }
        return Math.min(depth, HARD_MAX_DEPTH);
    }

    private String buildTitle(InterviewKbCrawlRequest req, URI seedUri) {
        String explicitTitle = clean(req.getTitle());
        if (!explicitTitle.isEmpty()) {
            return cut(explicitTitle, 255);
        }
        String topic = clean(req.getTopic());
        String host = normalizeHost(seedUri.getHost());
        String base = topic.isEmpty() ? ("crawler-kb-" + host) : topic;
        return cut(base + "-" + TITLE_TIME_FORMAT.format(Instant.now()), 255);
    }

    private String buildFilename(URI seedUri) {
        String host = normalizeHost(seedUri.getHost()).replaceAll("[^a-z0-9.-]", "_");
        if (host.isBlank()) {
            host = "unknown";
        }
        return "crawl_" + host + "_" + Instant.now().toEpochMilli() + ".txt";
    }

    private String normalizeHost(String host) {
        String value = clean(host).toLowerCase(Locale.ROOT);
        if (value.startsWith("www.")) {
            return value.substring(4);
        }
        return value;
    }

    private void addError(List<String> errors, String value) {
        if (errors.size() >= MAX_ERRORS) {
            return;
        }
        String msg = clean(value);
        if (!msg.isEmpty()) {
            errors.add(msg);
        }
    }

    private String cut(String text, int max) {
        String value = clean(text);
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static class FetchContext {
        private final String cookieHeader;
        private final String referer;
        private final String authHeader;

        private FetchContext(String cookieHeader, String referer, String authHeader) {
            this.cookieHeader = cookieHeader;
            this.referer = referer;
            this.authHeader = authHeader;
        }
    }

    private static class CookiePair {
        private final String key;
        private final String value;

        private CookiePair(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private static class CrawlNode {
        private final String url;
        private final int depth;

        private CrawlNode(String url, int depth) {
            this.url = url;
            this.depth = depth;
        }
    }
}
