package com.resumeai.dto;

/**
 * 保存简历版本请求
 */
public class SaveVersionRequest {
    private String title;
    private String content;
    private String targetRole;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }
}
