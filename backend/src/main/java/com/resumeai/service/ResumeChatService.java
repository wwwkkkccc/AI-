package com.resumeai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.dto.AnalyzeResponse;
import com.resumeai.dto.ChatMessageResponse;
import com.resumeai.dto.ChatSessionResponse;
import com.resumeai.model.AnalysisRecord;
import com.resumeai.model.ChatMessage;
import com.resumeai.model.ChatSession;
import com.resumeai.model.UserAccount;
import com.resumeai.repository.AnalysisRecordRepository;
import com.resumeai.repository.ChatMessageRepository;
import com.resumeai.repository.ChatSessionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeChatService {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_ASSISTANT = "ASSISTANT";

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AnalysisRecordRepository analysisRecordRepository;
    private final ObjectMapper objectMapper;
    private final LlmClient llmClient;

    public ResumeChatService(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            AnalysisRecordRepository analysisRecordRepository,
            ObjectMapper objectMapper,
            LlmClient llmClient) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.analysisRecordRepository = analysisRecordRepository;
        this.objectMapper = objectMapper;
        this.llmClient = llmClient;
    }

    @Transactional
    public ChatSessionResponse startSession(Long analysisId, UserAccount user, boolean adminMode) {
        AnalysisRecord record = null;
        if (analysisId != null) {
            record = loadRecord(analysisId, user, adminMode);
        }

        ChatSession session = new ChatSession();
        session.setUserId(user.getId());
        session.setAnalysisId(analysisId);
        session.setCreatedAt(Instant.now());
        session = chatSessionRepository.save(session);

        saveMessage(session.getId(), ROLE_ASSISTANT, buildWelcome(record));
        return toSessionResponse(session);
    }

    @Transactional
    public ChatSessionResponse sendMessage(Long sessionId, String message, UserAccount user, boolean adminMode) {
        ChatSession session = loadSession(sessionId);
        verifySessionAccess(session, user, adminMode);

        String question = clean(message);
        if (question.isEmpty()) {
            throw new IllegalArgumentException("message is required");
        }
        saveMessage(sessionId, ROLE_USER, question);

        AnalysisRecord record = null;
        if (session.getAnalysisId() != null) {
            record = loadRecord(session.getAnalysisId(), user, adminMode);
        }

        String answer = callAssistant(sessionId, record);
        if (answer == null) {
            answer = "Rewrite this experience using STAR: context, ownership, action, and measurable result.";
        }
        saveMessage(sessionId, ROLE_ASSISTANT, answer);
        return toSessionResponse(session);
    }

    @Transactional(readOnly = true)
    public ChatSessionResponse getMessages(Long sessionId, UserAccount user, boolean adminMode) {
        ChatSession session = loadSession(sessionId);
        verifySessionAccess(session, user, adminMode);
        return toSessionResponse(session);
    }

    private String callAssistant(Long sessionId, AnalysisRecord record) {
        List<ChatMessage> history = chatMessageRepository.findBySessionIdOrderByIdAsc(sessionId);
        int start = Math.max(0, history.size() - 12);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", """
                You are a resume optimization assistant.
                Rules:
                1. Provide directly usable resume wording.
                2. Prefer STAR phrasing.
                3. Include concrete rewrite examples.
                4. If information is missing, request exact fields to fill.
                5. Do not fabricate candidate experience.
                """));
        if (record != null) {
            messages.add(Map.of("role", "system", "content", buildAnalysisContext(record)));
        }

        for (int i = start; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
            String role = ROLE_USER.equals(msg.getRole()) ? "user" : "assistant";
            messages.add(Map.of("role", role, "content", cut(msg.getContent(), 1800)));
        }
        return llmClient.chat(messages, 0.2, 45);
    }

    private String buildWelcome(AnalysisRecord record) {
        if (record == null) {
            return "Chat session created. You can ask: rewrite this bullet in STAR format.";
        }
        String role = clean(record.getTargetRole());
        return """
                Analysis record loaded (ID=%s, role=%s).
                You can ask:
                1. Rewrite this experience with STAR.
                2. How to cover missing keywords naturally.
                3. Provide two rewrite variants with different emphasis.
                """.formatted(record.getId(), role.isEmpty() ? "-" : role);
    }

    private String buildAnalysisContext(AnalysisRecord record) {
        String summary = "";
        String missing = "";
        String matched = "";
        try {
            AnalyzeResponse response = objectMapper.readValue(clean(record.getResultJson()), AnalyzeResponse.class);
            if (response.getOptimized() != null) {
                summary = clean(response.getOptimized().getSummary());
            }
            matched = response.getMatchedKeywords() == null ? "" : response.getMatchedKeywords().toString();
            missing = response.getMissingKeywords() == null ? "" : response.getMissingKeywords().toString();
        } catch (Exception ignore) {
            // ignore parse errors
        }

        return """
                Analysis context:
                - target_role: %s
                - score: %s
                - coverage: %s
                - optimized_summary: %s
                - matched_keywords: %s
                - missing_keywords: %s
                - resume_excerpt: %s
                - jd_excerpt: %s
                """.formatted(
                clean(record.getTargetRole()),
                record.getScore(),
                record.getCoverage(),
                summary,
                matched,
                missing,
                cut(clean(record.getResumeText()), 1800),
                cut(clean(record.getJdText()), 1200)
        );
    }

    private ChatMessage saveMessage(Long sessionId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(clean(content));
        message.setCreatedAt(Instant.now());
        return chatMessageRepository.save(message);
    }

    private ChatSessionResponse toSessionResponse(ChatSession session) {
        ChatSessionResponse response = new ChatSessionResponse();
        response.setSessionId(session.getId());
        response.setAnalysisId(session.getAnalysisId());
        response.setCreatedAt(session.getCreatedAt());

        List<ChatMessageResponse> rows = new ArrayList<>();
        for (ChatMessage msg : chatMessageRepository.findBySessionIdOrderByIdAsc(session.getId())) {
            ChatMessageResponse item = new ChatMessageResponse();
            item.setId(msg.getId());
            item.setRole(msg.getRole());
            item.setContent(msg.getContent());
            item.setCreatedAt(msg.getCreatedAt());
            rows.add(item);
        }
        response.setMessages(rows);
        return response;
    }

    private ChatSession loadSession(Long sessionId) {
        return chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("chat session not found"));
    }

    private AnalysisRecord loadRecord(Long analysisId, UserAccount user, boolean adminMode) {
        AnalysisRecord record = analysisRecordRepository.findById(analysisId)
                .orElseThrow(() -> new IllegalArgumentException("analysis record not found"));
        if (!adminMode && !record.getUserId().equals(user.getId())) {
            throw new ForbiddenException("cannot access this analysis record");
        }
        return record;
    }

    private void verifySessionAccess(ChatSession session, UserAccount user, boolean adminMode) {
        if (adminMode) {
            return;
        }
        if (!session.getUserId().equals(user.getId())) {
            throw new ForbiddenException("cannot access this chat session");
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

