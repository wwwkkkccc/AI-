package com.resumeai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 批量简历分析批次表
 */
@Entity
@Table(
        name = "analysis_batches",
        indexes = {
                @Index(name = "idx_analysis_batches_user_id", columnList = "user_id"),
                @Index(name = "idx_analysis_batches_status", columnList = "status")
        }
)
public class AnalysisBatch {
    // 批次ID（UUID字符串）
    @Id
    @Column(name = "id", length = 64)
    private String id;

    // 提交批次的用户ID
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 批次中的任务总数
    @Column(name = "job_count", nullable = false)
    private Integer jobCount;

    // 已完成的任务数
    @Column(name = "completed_count", nullable = false)
    private Integer completedCount;

    // 批次状态（PENDING/PROCESSING/DONE）
    @Column(name = "status", nullable = false, length = 32)
    private String status;

    // 目标岗位名称
    @Column(name = "target_role", length = 255)
    private String targetRole;

    // 职位描述文本
    @Lob
    @Column(name = "jd_text", columnDefinition = "LONGTEXT")
    private String jdText;

    // 批次创建时间
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // 批次完成时间
    @Column(name = "finished_at")
    private Instant finishedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getJobCount() {
        return jobCount;
    }

    public void setJobCount(Integer jobCount) {
        this.jobCount = jobCount;
    }

    public Integer getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(Integer completedCount) {
        this.completedCount = completedCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
