package com.resumeai.dto;

import java.time.Instant;

/**
 * 简历分析历史记录项 DTO，用于返回单条分析历史的详细信息
 */
public class AnalysisHistoryItem {
    /** 分析记录 ID */
    private Long id;
    /** 用户 ID */
    private Long userId;
    /** 用户名 */
    private String username;
    /** 上传的简历文件名 */
    private String filename;
    /** 目标岗位 */
    private String targetRole;
    /** 简历匹配评分 */
    private Double score;
    /** 关键词覆盖率 */
    private Double coverage;
    /** 优化建议摘要 */
    private String optimizedSummary;
    /** 是否使用了 LLM 模型进行分析 */
    private Boolean modelUsed;
    /** 简历内容预览（截断） */
    private String resumePreview;
    /** 职位描述预览（截断） */
    private String jdPreview;
    /** 简历完整文本 */
    private String resumeText;
    /** 职位描述完整文本 */
    private String jdText;
    /** 分析创建时间 */
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getCoverage() {
        return coverage;
    }

    public void setCoverage(Double coverage) {
        this.coverage = coverage;
    }

    public String getOptimizedSummary() {
        return optimizedSummary;
    }

    public void setOptimizedSummary(String optimizedSummary) {
        this.optimizedSummary = optimizedSummary;
    }

    public Boolean getModelUsed() {
        return modelUsed;
    }

    public void setModelUsed(Boolean modelUsed) {
        this.modelUsed = modelUsed;
    }

    public String getResumePreview() {
        return resumePreview;
    }

    public void setResumePreview(String resumePreview) {
        this.resumePreview = resumePreview;
    }

    public String getJdPreview() {
        return jdPreview;
    }

    public void setJdPreview(String jdPreview) {
        this.jdPreview = jdPreview;
    }

    public String getResumeText() {
        return resumeText;
    }

    public void setResumeText(String resumeText) {
        this.resumeText = resumeText;
    }

    public String getJdText() {
        return jdText;
    }

    public void setJdText(String jdText) {
        this.jdText = jdText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
