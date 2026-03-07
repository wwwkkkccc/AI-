package com.resumeai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 简历分析任务表，记录每次简历分析的异步任务信息及执行状态
 */
@Entity
@Table(
        name = "analysis_jobs",
        indexes = {
                @Index(name = "idx_analysis_jobs_status_created", columnList = "status,created_at"),
                @Index(name = "idx_analysis_jobs_user_id", columnList = "user_id")
        }
)
public class AnalysisJob {
    // 任务ID（字符串类型主键）
    @Id
    @Column(name = "id", length = 64)
    private String id;

    // 提交任务的用户ID
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 提交任务的用户名
    @Column(name = "username", nullable = false, length = 64)
    private String username;

    // 上传的简历文件名
    @Column(name = "filename", length = 255)
    private String filename;

    // 简历文件在服务器上的存储路径
    @Column(name = "file_path", nullable = false, length = 600)
    private String filePath;

    // 目标岗位名称
    @Column(name = "target_role", length = 255)
    private String targetRole;

    // 职位描述（JD）文本
    @Lob
    @Column(name = "jd_text", nullable = false, columnDefinition = "LONGTEXT")
    private String jdText;

    // 任务优先级
    @Column(name = "priority_level", nullable = false)
    private Integer priorityLevel;

    // 任务状态（如 pending、running、finished、failed）
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    // 分析结果JSON
    @Lob
    @Column(name = "result_json", columnDefinition = "LONGTEXT")
    private String resultJson;

    // 任务失败时的错误信息
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    // 任务创建时间
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // 任务开始执行时间
    @Column(name = "started_at")
    private Instant startedAt;

    // 任务完成时间
    @Column(name = "finished_at")
    private Instant finishedAt;

    // 所属批次ID（可选）
    @Column(name = "batch_id", length = 64)
    private String batchId;

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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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

    public Integer getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(Integer priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }
}
