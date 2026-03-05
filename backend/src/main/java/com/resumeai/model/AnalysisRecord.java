package com.resumeai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 简历分析结果记录表，保存每次分析完成后的评分、覆盖率等详细结果
 */
@Entity
@Table(name = "analysis_records")
public class AnalysisRecord {
    // 自增主键ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 记录创建时间
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // 简历文件名
    @Column(name = "filename", length = 255)
    private String filename;

    // 所属用户ID
    @Column(name = "user_id")
    private Long userId;

    // 所属用户名
    @Column(name = "username", length = 64)
    private String username;

    // 目标岗位名称
    @Column(name = "target_role", length = 255)
    private String targetRole;

    // 职位描述（JD）文本
    @Lob
    @Column(name = "jd_text", nullable = false, columnDefinition = "LONGTEXT")
    private String jdText;

    // 简历原文文本
    @Lob
    @Column(name = "resume_text", nullable = false, columnDefinition = "LONGTEXT")
    private String resumeText;

    // 简历匹配评分
    @Column(name = "score", nullable = false)
    private Double score;

    // JD关键词覆盖率
    @Column(name = "coverage", nullable = false)
    private Double coverage;

    // 优化建议摘要
    @Column(name = "optimized_summary", length = 1000)
    private String optimizedSummary;

    // 是否使用了AI模型进行分析
    @Column(name = "model_used")
    private Boolean modelUsed = false;

    // 完整分析结果JSON
    @Lob
    @Column(name = "result_json", columnDefinition = "LONGTEXT")
    private String resultJson;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
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

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getJdText() {
        return jdText;
    }

    public void setJdText(String jdText) {
        this.jdText = jdText;
    }

    public String getResumeText() {
        return resumeText;
    }

    public void setResumeText(String resumeText) {
        this.resumeText = resumeText;
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

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }
}
