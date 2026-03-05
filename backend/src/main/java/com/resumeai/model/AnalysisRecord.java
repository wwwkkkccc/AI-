package com.resumeai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "analysis_records")
public class AnalysisRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "filename", length = 255)
    private String filename;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 64)
    private String username;

    @Column(name = "target_role", length = 255)
    private String targetRole;

    @Lob
    @Column(name = "jd_text", nullable = false, columnDefinition = "LONGTEXT")
    private String jdText;

    @Lob
    @Column(name = "resume_text", nullable = false, columnDefinition = "LONGTEXT")
    private String resumeText;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "coverage", nullable = false)
    private Double coverage;

    @Column(name = "optimized_summary", length = 1000)
    private String optimizedSummary;

    @Column(name = "model_used")
    private Boolean modelUsed = false;

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
