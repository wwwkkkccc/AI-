package com.resumeai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.resumeai.dto.InterviewMessageResponse;
import com.resumeai.dto.InterviewSessionResponse;
import com.resumeai.model.MockInterviewMessage;
import com.resumeai.model.MockInterviewSession;
import com.resumeai.model.UserAccount;
import com.resumeai.repository.MockInterviewMessageRepository;
import com.resumeai.repository.MockInterviewSessionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 妯℃嫙闈㈣瘯鏈嶅姟銆? * <p>
 * 鑱岃矗锛? * 1. 鍒涘缓闈㈣瘯浼氳瘽锛屼粠棰樺簱鎶介 + LLM 鐢熸垚寮€鍦洪棶棰? * 2. 鎺ユ敹鐢ㄦ埛鍥炵瓟锛孡LM 璇勫垎骞剁敓鎴愪笅涓€涓棶棰? * 3. 缁撴潫闈㈣瘯锛孡LM 鐢熸垚鎬昏瘎鎶ュ憡
 * 4. 鏌ヨ闈㈣瘯鍘嗗彶娑堟伅
 * </p>
 */
@Service
public class MockInterviewService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_FINISHED = "FINISHED";
    private static final String ROLE_AI = "AI";
    private static final String ROLE_USER = "USER";
    private static final int MAX_QUESTIONS = 10;

    private final MockInterviewSessionRepository sessionRepository;
    private final MockInterviewMessageRepository messageRepository;
    private final InterviewQuestionKbService kbService;
    private final LlmClient llmClient;

    public MockInterviewService(
            MockInterviewSessionRepository sessionRepository,
            MockInterviewMessageRepository messageRepository,
            InterviewQuestionKbService kbService,
            LlmClient llmClient) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.kbService = kbService;
        this.llmClient = llmClient;
    }

    /** 寮€濮嬩竴鍦烘ā鎷熼潰璇?*/
    @Transactional
    public InterviewSessionResponse startInterview(
            String targetRole,
            String resumeText,
            String jdText,
            List<String> missingKeywords,
            UserAccount user) {
        String role = clean(targetRole);
        String resume = clean(resumeText);
        String jd = clean(jdText);
        List<String> normalizedMissingKeywords = normalizeKeywords(missingKeywords, 12);
        if (role.isEmpty()) {
            throw new IllegalArgumentException("鐩爣宀椾綅涓嶈兘涓虹┖");
        }
        if (resume.length() < 20 && jd.length() < 20) {
            throw new IllegalArgumentException("璇锋彁渚涚畝鍘嗘枃鏈垨宀椾綅鎻忚堪");
        }

        MockInterviewSession session = new MockInterviewSession();
        session.setUserId(user.getId());
        session.setTargetRole(role);
        session.setResumeText(cut(resume, 8000));
        session.setJdText(cut(jd, 5000));
        session.setStatus(STATUS_ACTIVE);
        session.setQuestionCount(0);
        session.setCreatedAt(Instant.now());
        session = sessionRepository.save(session);

        // 浠庨搴撴绱㈢浉鍏抽潰璇曢浣滀负鍙傝€?
        List<String> kbQuestions = kbService.retrieveQuestions(role, jd, normalizedMissingKeywords, 5);

        // 璋冪敤 LLM 鐢熸垚绗竴涓潰璇曢棶棰?
        String firstQuestion = generateFirstQuestion(role, resume, jd, normalizedMissingKeywords, kbQuestions);

        MockInterviewMessage aiMsg = saveMessage(session.getId(), ROLE_AI, firstQuestion, null);
        session.setQuestionCount(1);
        sessionRepository.save(session);

        return buildResponse(session, toMessageResponse(aiMsg));
    }

    /** 鐢ㄦ埛鍥炵瓟褰撳墠闂锛孉I 璇勫垎骞舵彁鍑轰笅涓€涓棶棰?*/
    @Transactional
    public InterviewSessionResponse answer(Long sessionId, String userAnswer, UserAccount user) {
        MockInterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("interview session not found"));
        if (!session.getUserId().equals(user.getId())) {
            throw new ForbiddenException("cannot access this interview session");
        }
        if (STATUS_FINISHED.equals(session.getStatus())) {
            throw new IllegalArgumentException("interview already finished");
        }
        String answer = clean(userAnswer);
        if (answer.isEmpty()) {
            throw new IllegalArgumentException("answer cannot be empty");
        }

        // 淇濆瓨鐢ㄦ埛鍥炵瓟
        saveMessage(sessionId, ROLE_USER, answer, null);

        // 鑾峰彇鍘嗗彶瀵硅瘽鏋勫缓涓婁笅鏂?
        List<MockInterviewMessage> history = messageRepository.findBySessionIdOrderByIdAsc(sessionId);

        // 璋冪敤 LLM 璇勫垎 + 鐢熸垚涓嬩竴涓棶棰?
        String llmResponse = callLlmForEvaluation(session, history, answer);
        JsonNode parsed = llmClient.parseJson(llmResponse);

        int score = 0;
        String feedback = "";
        String nextQuestion = "";

        if (parsed != null) {
            score = parsed.path("score").asInt(60);
            feedback = parsed.path("feedback").asText("");
            nextQuestion = parsed.path("nextQuestion").asText("");
        }
        if (feedback.isEmpty()) {
            feedback = "回答不错，可以再补充更具体的细节和量化结果。";
        }

        int questionCount = session.getQuestionCount() == null ? 0 : session.getQuestionCount();
        boolean shouldEnd = questionCount >= MAX_QUESTIONS || nextQuestion.isEmpty();

        String aiContent;
        if (shouldEnd) {
            aiContent = "**评分：" + score + "/100**\n\n"
                    + feedback
                    + "\n\n本轮问答已结束，请点击「结束面试」查看总评。";
        } else {
            aiContent = "**评分：" + score + "/100**\n\n"
                    + feedback
                    + "\n\n---\n\n**下一题：**"
                    + nextQuestion;
            session.setQuestionCount(questionCount + 1);
        }

        MockInterviewMessage aiMsg = saveMessage(sessionId, ROLE_AI, aiContent, score);
        sessionRepository.save(session);

        return buildResponse(session, toMessageResponse(aiMsg));
    }

    /** 缁撴潫闈㈣瘯锛岀敓鎴愭€昏瘎鎶ュ憡 */
    @Transactional
    public InterviewSessionResponse endInterview(Long sessionId, UserAccount user) {
        MockInterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("interview session not found"));
        if (!session.getUserId().equals(user.getId())) {
            throw new ForbiddenException("cannot access this interview session");
        }
        if (STATUS_FINISHED.equals(session.getStatus())) {
            return buildResponseWithMessages(session);
        }

        List<MockInterviewMessage> history = messageRepository.findBySessionIdOrderByIdAsc(sessionId);

        // 璁＄畻骞冲潎鍒?
        double avgScore = history.stream()
                .filter(m -> ROLE_AI.equals(m.getRole()) && m.getScore() != null)
                .mapToInt(MockInterviewMessage::getScore)
                .average()
                .orElse(0);

        // 璋冪敤 LLM 鐢熸垚鎬昏瘎
        String report = generateReport(session, history, avgScore);

        MockInterviewMessage reportMsg = saveMessage(sessionId, ROLE_AI, report, null);

        session.setStatus(STATUS_FINISHED);
        session.setTotalScore(Math.round(avgScore * 100.0) / 100.0);
        session.setFinishedAt(Instant.now());
        sessionRepository.save(session);

        InterviewSessionResponse resp = buildResponse(session, toMessageResponse(reportMsg));
        resp.setReport(report);
        return resp;
    }

    /** 鏌ヨ闈㈣瘯浼氳瘽鐨勫畬鏁存秷鎭垪琛?*/
    @Transactional(readOnly = true)
    public InterviewSessionResponse getMessages(Long sessionId, UserAccount user) {
        MockInterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("interview session not found"));
        if (!session.getUserId().equals(user.getId())) {
            throw new ForbiddenException("cannot access this interview session");
        }
        return buildResponseWithMessages(session);
    }

    /** 鏌ヨ鐢ㄦ埛鐨勯潰璇曞巻鍙插垪琛?*/
    @Transactional(readOnly = true)
    public List<InterviewSessionResponse> listSessions(UserAccount user) {
        List<MockInterviewSession> sessions = sessionRepository.findByUserIdOrderByIdDesc(user.getId());
        return sessions.stream().map(s -> {
            InterviewSessionResponse r = new InterviewSessionResponse();
            r.setSessionId(s.getId());
            r.setStatus(s.getStatus());
            r.setTargetRole(s.getTargetRole());
            r.setQuestionCount(s.getQuestionCount());
            r.setTotalScore(s.getTotalScore());
            r.setCreatedAt(s.getCreatedAt());
            r.setFinishedAt(s.getFinishedAt());
            return r;
        }).toList();
    }

    // ==================== LLM 璋冪敤 ====================

    private String generateFirstQuestion(
            String targetRole,
            String resumeText,
            String jdText,
            List<String> missingKeywords,
            List<String> kbQuestions) {
        String missingRef = missingKeywords.isEmpty() ? "none" : String.join(", ", missingKeywords);
        String kbRef = kbQuestions.isEmpty() ? "none" : String.join("\n", kbQuestions);
        String prompt = """
                You are a senior technical interviewer for the %s role.
                Candidate resume excerpt:
                %s

                Job description:
                %s

                Missing keywords from ATS analysis:
                %s

                Question bank references:
                %s

                Generate exactly one first interview question in Chinese.
                Requirements:
                1. Tie it to role requirements, project context, and missing keywords.
                2. Prefer a practical question instead of a generic one.
                3. Output only the question text.
                """.formatted(targetRole, cut(resumeText, 3000), cut(jdText, 2000), cut(missingRef, 800), kbRef);

        String result = llmClient.chat(List.of(
                Map.of("role", "system", "content", "You are a technical interviewer and only output one question."),
                Map.of("role", "user", "content", prompt)
        ), 0.3, 30);

        return result != null ? result : "请介绍一个你最近做过的项目，以及你如何解决其中的关键技术难题。";
    }

    private String callLlmForEvaluation(MockInterviewSession session, List<MockInterviewMessage> history, String latestAnswer) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", """
                You are a senior technical interviewer for the %s role.
                Evaluate the candidate's latest answer and provide the next question.
                Return strict JSON only:
                {
                  "score": integer between 0 and 100,
                  "feedback": "short feedback in Chinese, point out strengths and gaps",
                  "nextQuestion": "next interview question in Chinese"
                }
                Scoring dimensions:
                - Technical depth: 30
                - Clarity of expression: 20
                - Practical experience: 30
                - Reasoning and structure: 20
                """.formatted(clean(session.getTargetRole()))));

        // add latest 10 rounds as context
        int start = Math.max(0, history.size() - 20);
        for (int i = start; i < history.size(); i++) {
            MockInterviewMessage msg = history.get(i);
            String role = ROLE_AI.equals(msg.getRole()) ? "assistant" : "user";
            messages.add(Map.of("role", role, "content", cut(msg.getContent(), 2000)));
        }

        return llmClient.chat(messages, 0.2, 40);
    }

    private String generateReport(MockInterviewSession session, List<MockInterviewMessage> history, double avgScore) {
        StringBuilder context = new StringBuilder();
        for (MockInterviewMessage msg : history) {
            String label = ROLE_AI.equals(msg.getRole()) ? "Interviewer" : "Candidate";
            context.append(label).append(": ").append(cut(msg.getContent(), 500)).append("\n\n");
        }

        String prompt = """
                以下是 %s 岗位的一场模拟面试完整记录：

                %s

                平均得分：%.1f

                请用中文 Markdown 生成面试总评，包含：
                1. 总体评价（2-3 句）
                2. 技术能力评价
                3. 表达与沟通评价
                4. 亮点（2-3 条）
                5. 改进点（2-3 条）
                6. 备战建议（2-3 条，可执行）
                """.formatted(clean(session.getTargetRole()), cut(context.toString(), 6000), avgScore);

        String result = llmClient.chat(List.of(
                Map.of("role", "system", "content", "你是面试评估专家，请输出专业、具体、可执行的总结报告。"),
                Map.of("role", "user", "content", prompt)
        ), 0.3, 45);

        if (result != null) {
            return result;
        }
        return "## 面试总评\n\n平均得分："
                + Math.round(avgScore)
                + "/100\n\n共回答 "
                + (session.getQuestionCount() == null ? 0 : session.getQuestionCount())
                + " 道题。\n\n"
                + "- 建议 1：使用 STAR 结构补充项目细节。\n"
                + "- 建议 2：增加关键指标和量化结果。\n"
                + "- 建议 3：回答时先结论后展开，突出技术决策。";
    }

    // ==================== 宸ュ叿鏂规硶 ====================

    private MockInterviewMessage saveMessage(Long sessionId, String role, String content, Integer score) {
        MockInterviewMessage msg = new MockInterviewMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setScore(score);
        msg.setCreatedAt(Instant.now());
        return messageRepository.save(msg);
    }

    private InterviewMessageResponse toMessageResponse(MockInterviewMessage msg) {
        InterviewMessageResponse r = new InterviewMessageResponse();
        r.setId(msg.getId());
        r.setRole(msg.getRole());
        r.setContent(msg.getContent());
        r.setScore(msg.getScore());
        r.setCreatedAt(msg.getCreatedAt());
        return r;
    }

    private InterviewSessionResponse buildResponse(MockInterviewSession session, InterviewMessageResponse latestAi) {
        InterviewSessionResponse r = new InterviewSessionResponse();
        r.setSessionId(session.getId());
        r.setStatus(session.getStatus());
        r.setTargetRole(session.getTargetRole());
        r.setQuestionCount(session.getQuestionCount());
        r.setTotalScore(session.getTotalScore());
        r.setCreatedAt(session.getCreatedAt());
        r.setFinishedAt(session.getFinishedAt());
        r.setLatestAiMessage(latestAi);
        return r;
    }

    private InterviewSessionResponse buildResponseWithMessages(MockInterviewSession session) {
        List<MockInterviewMessage> msgs = messageRepository.findBySessionIdOrderByIdAsc(session.getId());
        InterviewSessionResponse r = buildResponse(session, null);
        r.setMessages(msgs.stream().map(this::toMessageResponse).toList());
        return r;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private List<String> normalizeKeywords(List<String> missingKeywords, int maxSize) {
        if (missingKeywords == null || missingKeywords.isEmpty() || maxSize <= 0) {
            return List.of();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String keyword : missingKeywords) {
            String normalized = clean(keyword);
            if (normalized.isEmpty()) {
                continue;
            }
            unique.add(cut(normalized, 60));
            if (unique.size() >= maxSize) {
                break;
            }
        }
        return new ArrayList<>(unique);
    }

    private String cut(String text, int max) {
        String value = clean(text);
        return value.length() <= max ? value : value.substring(0, max);
    }
}

