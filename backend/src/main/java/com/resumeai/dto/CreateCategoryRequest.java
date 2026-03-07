package com.resumeai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建分类请求
 */
public class CreateCategoryRequest {
    @NotBlank(message = "category name is required")
    @Size(max = 128, message = "category name too long")
    private String name;

    private Long parentId;

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
}
