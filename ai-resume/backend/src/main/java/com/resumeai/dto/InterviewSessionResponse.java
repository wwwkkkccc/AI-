package com.resumeai.dto;

import java.time.Instant;
import java.util.List;

/**
 * 模拟面试会话状态响应（含开始/回答/结束的统一返回）。
 */
public class InterviewSessionResponse {
    private Long sessionId;
    private String status;
    private String targetRole;
    private Integer questionCount;
    private Double totalScore;
    private Instant createdAt;
    private Instant finishedAt;
    /** 最新一条 AI 消息（开始/回答时返回） */
    private InterviewMessageResponse latestAiMessage;
    /** 结束面试时的总评报告 */
    private String report;
    /** 完整消息列表（查询历史时返回） */
    private List<InterviewMessageResponse> messages;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
    public Double getTotalScore() { return totalScore; }
    public void setTotalScore(Double totalScore) { this.totalScore = totalScore; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
    public InterviewMessageResponse getLatestAiMessage() { return latestAiMessage; }
    public void setLatestAiMessage(InterviewMessageResponse latestAiMessage) { this.latestAiMessage = latestAiMessage; }
    public String getReport() { return report; }
    public void setReport(String report) { this.report = report; }
    public List<InterviewMessageResponse> getMessages() { return messages; }
    public void setMessages(List<InterviewMessageResponse> messages) { this.messages = messages; }
}
