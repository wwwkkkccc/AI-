package com.resumeai.dto;

import java.util.List;

/**
 * 开始模拟面试请求。
 */
public class StartInterviewRequest {
    private String targetRole;
    private String resumeText;
    private String jdText;
    private List<String> missingKeywords;

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    public String getResumeText() { return resumeText; }
    public void setResumeText(String resumeText) { this.resumeText = resumeText; }
    public String getJdText() { return jdText; }
    public void setJdText(String jdText) { this.jdText = jdText; }
    public List<String> getMissingKeywords() { return missingKeywords; }
    public void setMissingKeywords(List<String> missingKeywords) { this.missingKeywords = missingKeywords; }
}
