package com.resumeai.dto;

import java.time.Instant;

/**
 * 面试知识库文档项 DTO，用于返回单个知识库文档的基本信息
 */
public class InterviewKbDocItem {
    /** 文档 ID */
    private Long id;
    /** 文档标题 */
    private String title;
    /** 文件名 */
    private String filename;
    /** 文档中包含的面试题数量 */
    private Integer questionCount;
    /** 上传者用户 ID */
    private Long uploadedById;
    /** 上传者用户名 */
    private String uploadedBy;
    /** 文档创建时间 */
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public Long getUploadedById() {
        return uploadedById;
    }

    public void setUploadedById(Long uploadedById) {
        this.uploadedById = uploadedById;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
