package com.resumeai.dto;

import jakarta.validation.constraints.NotBlank;

public class GenerateResumeRequest {

    @NotBlank(message = "target_role is required")
    private String targetRole;

    @NotBlank(message = "jd_text is required")
    private String jdText;

    private String userBackground;

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

    public String getUserBackground() {
        return userBackground;
    }

    public void setUserBackground(String userBackground) {
        this.userBackground = userBackground;
    }
}
