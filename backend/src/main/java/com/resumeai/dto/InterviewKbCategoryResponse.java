package com.resumeai.dto;

import java.time.Instant;
import java.util.List;

/**
 * 面试题库分类响应（树形结构）
 */
public class InterviewKbCategoryResponse {
    private Long id;
    private String name;
    private Long parentId;
    private int questionCount;
    private Instant createdAt;
    private List<InterviewKbCategoryResponse> children;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<InterviewKbCategoryResponse> getChildren() {
        return children;
    }

    public void setChildren(List<InterviewKbCategoryResponse> children) {
        this.children = children;
    }
}
