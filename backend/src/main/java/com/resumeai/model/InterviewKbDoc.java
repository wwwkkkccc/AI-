package com.resumeai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 面试知识库文档表，记录上传的面试题文档的元信息
 */
@Entity
@Table(name = "interview_kb_docs")
public class InterviewKbDoc {
    // 自增主键ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 文档标题
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    // 上传的原始文件名
    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    // 上传者用户ID
    @Column(name = "uploaded_by_id")
    private Long uploadedById;

    // 上传者用户名
    @Column(name = "uploaded_by", length = 64)
    private String uploadedBy;

    // 文档中包含的面试题数量
    @Column(name = "question_count", nullable = false)
    private Integer questionCount;

    // 文档创建时间
    @Column(name = "created_at", nullable = false)
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

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
