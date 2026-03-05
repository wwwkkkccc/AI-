package com.resumeai.dto;

public class GeneratedResumeResponse {

    private String mode;
    private String markdown;
    private Boolean modelUsed;
    private Long analysisId;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getMarkdown() {
        return markdown;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }

    public Boolean getModelUsed() {
        return modelUsed;
    }

    public void setModelUsed(Boolean modelUsed) {
        this.modelUsed = modelUsed;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Long analysisId) {
        this.analysisId = analysisId;
    }
}
