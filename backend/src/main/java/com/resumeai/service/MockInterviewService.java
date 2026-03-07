package com.resumeai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.resumeai.dto.InterviewMessageResponse;
import com.resumeai.dto.InterviewSessionsResponse;
import com.resumeai.model.InterviewKbItem;
import com.resumeai.model.MockInterviewMessage;
import com.resumeai.model.MockInterviewSession;
import com.resumeai.model.UserAccount;
import com.resumeai.repository.MockInterviewMessageRepository;
import com.resumeai.repository.MockInterviewSessionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 模拟面试服务
 */
@Service
public class MockInterviewService {

    private final MockInterviewSessionRepository sessionRepository;
    private final MockInterviewMessageRepository messageRepository;
    private final InterviewQuestionKbService kbService;
    private final LlmClient llmClient;
    private final Random random = new Random();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));

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

    /**
     * 开始模拟面试
     */
    @Transactional
    public InterviewMessageResponse startInterview(String targetRole, String resumeText, String jdText, UserAccount user) {
        MockInterviewSession session = new MockInterviewSession();
        session.setUserId(user.getId());
        session.setTargetRole(clean(targetRole));
        session.setResumeText(clean(resumeText));
        session.setJdText(clean(jdText));
        session.setStatus("ACTIVE");
        session.setQuestionCount(0);
        session.setCreatedAt(Instant.now());
        session = sessionRepository.save(session);

        String firstQuestion = generateFirstQuestion(targetRole, resumeText, jdText);

        MockInterviewMessage aiMsg = new MockInterviewMessage();
        aiMsg.setSessionId(session.getId());
        aiMsg.setRole("AI");
        aiMsg.setContent(firstQuestion);
        aiMsg.setCreatedAt(Instant.now());
        messageRepository.save(aiMsg);

        session.setQuestionCount(1);
        sessionRepository.save(session);

        return buildMessageResponse(session);
    }

    /**
     * 回答问题
     */
    @Transactional
    public InterviewMessageResponse answerQuestion(Long sessionId, String userAnswer, UserAccount user) {
        MockInterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("面试会话不存在"));

        if (!session.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("无权访问此面试会话");
        }

        if (!"ACTIVE".equals(session.getStatus())) {
            throw new IllegalArgumentException("面试已结束");
        }

        MockInterviewMessage userMsg = new MockInterviewMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("USER");
        userMsg.setContent(clean(userAnswer));
        userMsg.setCreatedAt(Instant.now());
        messageRepository.save(userMsg);

        List<MockInterviewMessage> history = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        EvaluationResult eval = evaluateAnswer(session, history, userAnswer);

        userMsg.setScore(eval.score);
        userMsg.setFeedback(eval.feedback);
        messageRepository.save(userMsg);

        MockInterviewMessage aiMsg = new MockInterviewMessage();
        aiMsg.setSessionId(sessionId);
        aiMsg.setRole("AI");
        aiMsg.setContent(eval.nextQuestion);
        aiMsg.setCreatedAt(Instant.now());
        messageRepository.save(aiMsg);

        session.setQuestionCount(session.getQuestionCount() + 1);
        updateSessionScore(session);
        sessionRepository.save(session);

        return buildMessageResponse(session);
    }

    /**
     * 结束面试
     */
    @Transactional
    public InterviewMessageResponse endInterview(Long sessionId, UserAccount user) {
        MockInterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("面试会话不存在"));

        if (!session.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("无权访问此面试会话");
        }

        if (!"ACTIVE".equals(session.getStatus())) {
            throw new IllegalArgumentException("面试已结束");
        }

        session.setStatus("FINISHED");
        session.setFinishedAt(Instant.now());
        sessionRepository.save(session);

        return buildMessageResponse(session);
    }

    /**
     * 获取面试消息
     */
    public InterviewMessageResponse getMessages(Long sessionId, UserAccount user) {
        MockInterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("面试会话不存在"));

        if (!session.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("无权访问此面试会话");
        }

        return buildMessageResponse(session);
    }

    /**
     * 获取用户的面试会话列表
     */
    public InterviewSessionsResponse listSessions(UserAccount user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MockInterviewSession> pageResult = sessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        InterviewSessionsResponse response = new InterviewSessionsResponse();
        response.setPage(page);
        response.setSize(size);
        response.setTotal(pageResult.getTotalElements());
        response.setTotalPages(pageResult.getTotalPages());

        List<InterviewSessionsResponse.SessionItem> items = pageResult.getContent().stream()
                .map(this::toSessionItem)
                .collect(Collectors.toList());
        response.setItems(items);

        return response;
    }

    private String generateFirstQuestion(String targetRole, String resumeText, String jdText) {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是一位资深技术面试官，擅长针对候选人背景提出有深度的面试问题。");
        messages.add(systemMsg);

        Map<String, String> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", String.format(
                "目标岗位：%s\n简历：%s\nJD：%s\n\n请生成第一个面试问题，要求：1）针对候选人背景 2）有一定深度 3）直接返回问题文本，不要其他内容",
                targetRole,
                resumeText.length() > 500 ? resumeText.substring(0, 500) + "..." : resumeText,
                jdText.length() > 300 ? jdText.substring(0, 300) + "..." : jdText
        ));
        messages.add(userMsg);

        String llmResult = llmClient.chat(messages, 0.7, 15);
        if (llmResult != null && !llmResult.isBlank()) {
            return llmResult;
        }

        List<String> kbQuestions = kbService.retrieveQuestions(targetRole, "", new ArrayList<>(), 10);
        if (!kbQuestions.isEmpty()) {
            return kbQuestions.get(random.nextInt(kbQuestions.size()));
        }

        return "请先做一下自我介绍，重点介绍你在 " + targetRole + " 方面的经验和项目。";
    }

    private EvaluationResult evaluateAnswer(MockInterviewSession session, List<MockInterviewMessage> history, String userAnswer) {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是资深技术面试官。评估候选人回答并给出分数(0-100)、反馈和下一个问题。返回JSON格式：{\"score\":75,\"feedback\":\"...\",\"nextQuestion\":\"...\"}");
        messages.add(systemMsg);

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("目标岗位：").append(session.getTargetRole()).append("\n");
        contextBuilder.append("面试历史：\n");
        for (MockInterviewMessage msg : history) {
            contextBuilder.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        contextBuilder.append("USER: ").append(userAnswer).append("\n\n");
        contextBuilder.append("请评估最后一个回答，给出分数、反馈和下一个问题。");

        Map<String, String> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", contextBuilder.toString());
        messages.add(userMsg);

        String llmResult = llmClient.chat(messages, 0.7, 20);
        JsonNode json = llmClient.parseJsonObject(llmResult);

        if (json != null && json.has("score") && json.has("nextQuestion")) {
            EvaluationResult result = new EvaluationResult();
            result.score = json.path("score").asDouble(70.0);
            result.feedback = json.path("feedback").asText("回答基本合理");
            result.nextQuestion = json.path("nextQuestion").asText("");
            if (!result.nextQuestion.isBlank()) {
                return result;
            }
        }

        return fallbackEvaluation(session.getTargetRole(), userAnswer);
    }

    private EvaluationResult fallbackEvaluation(String targetRole, String userAnswer) {
        EvaluationResult result = new EvaluationResult();

        int length = userAnswer.length();
        if (length < 20) {
            result.score = 40.0;
            result.feedback = "回答过于简短，建议详细展开说明";
        } else if (length < 100) {
            result.score = 60.0;
            result.feedback = "回答基本合理，可以更详细地说明";
        } else {
            result.score = 75.0;
            result.feedback = "回答较为详细，表达清晰";
        }

        List<String> kbQuestions = kbService.retrieveQuestions(targetRole, "", new ArrayList<>(), 10);
        if (!kbQuestions.isEmpty()) {
            result.nextQuestion = kbQuestions.get(random.nextInt(kbQuestions.size()));
        } else {
            result.nextQuestion = "请描述一个你在项目中遇到的技术难题，以及你是如何解决的？";
        }

        return result;
    }

    private void updateSessionScore(MockInterviewSession session) {
        List<MockInterviewMessage> userMessages = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())
                .stream()
                .filter(m -> "USER".equals(m.getRole()) && m.getScore() != null)
                .collect(Collectors.toList());

        if (!userMessages.isEmpty()) {
            double avgScore = userMessages.stream()
                    .mapToDouble(MockInterviewMessage::getScore)
                    .average()
                    .orElse(0.0);
            session.setTotalScore(avgScore);
        }
    }

    private InterviewMessageResponse buildMessageResponse(MockInterviewSession session) {
        InterviewMessageResponse response = new InterviewMessageResponse();
        response.setSessionId(session.getId());
        response.setTargetRole(session.getTargetRole());
        response.setStatus(session.getStatus());
        response.setQuestionCount(session.getQuestionCount());
        response.setTotalScore(session.getTotalScore());

        List<MockInterviewMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        List<InterviewMessageResponse.MessageItem> items = messages.stream()
                .map(this::toMessageItem)
                .collect(Collectors.toList());
        response.setMessages(items);

        if ("FINISHED".equals(session.getStatus())) {
            response.setReport(generateReport(session, messages));
        }

        return response;
    }

    private InterviewMessageResponse.InterviewReport generateReport(MockInterviewSession session, List<MockInterviewMessage> messages) {
        List<MockInterviewMessage> userMessages = messages.stream()
                .filter(m -> "USER".equals(m.getRole()) && m.getScore() != null)
                .collect(Collectors.toList());

        if (userMessages.isEmpty()) {
            return null;
        }

        double avgScore = userMessages.stream()
                .mapToDouble(MockInterviewMessage::getScore)
                .average()
                .orElse(0.0);

        InterviewMessageResponse.InterviewReport report = new InterviewMessageResponse.InterviewReport();
        report.setReport(String.format("本次面试共回答 %d 个问题，平均得分 %.1f 分", userMessages.size(), avgScore));

        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        if (avgScore >= 80) {
            strengths.add("回答质量高，表达清晰");
            strengths.add("技术理解深入");
            suggestions.add("继续保持，可以尝试更有挑战性的岗位");
        } else if (avgScore >= 60) {
            strengths.add("基础知识掌握较好");
            weaknesses.add("部分问题回答不够深入");
            suggestions.add("加强实战经验积累");
            suggestions.add("多总结项目中的技术难点");
        } else {
            weaknesses.add("回答过于简短");
            weaknesses.add("技术深度不足");
            suggestions.add("系统学习相关技术栈");
            suggestions.add("多做项目实践");
        }

        report.setStrengths(strengths);
        report.setWeaknesses(weaknesses);
        report.setSuggestions(suggestions);

        return report;
    }

    private InterviewMessageResponse.MessageItem toMessageItem(MockInterviewMessage msg) {
        InterviewMessageResponse.MessageItem item = new InterviewMessageResponse.MessageItem();
        item.setId(msg.getId());
        item.setRole(msg.getRole());
        item.setContent(msg.getContent());
        item.setScore(msg.getScore());
        item.setFeedback(msg.getFeedback());
        item.setCreatedAt(FORMATTER.format(msg.getCreatedAt()));
        return item;
    }

    private InterviewSessionsResponse.SessionItem toSessionItem(MockInterviewSession session) {
        InterviewSessionsResponse.SessionItem item = new InterviewSessionsResponse.SessionItem();
        item.setId(session.getId());
        item.setTargetRole(session.getTargetRole());
        item.setStatus(session.getStatus());
        item.setQuestionCount(session.getQuestionCount());
        item.setTotalScore(session.getTotalScore());
        item.setCreatedAt(FORMATTER.format(session.getCreatedAt()));
        if (session.getFinishedAt() != null) {
            item.setFinishedAt(FORMATTER.format(session.getFinishedAt()));
        }
        return item;
    }

    private String clean(String s) {
        return s == null ? "" : s.trim();
    }

    private static class EvaluationResult {
        double score;
        String feedback;
        String nextQuestion;
    }
}
