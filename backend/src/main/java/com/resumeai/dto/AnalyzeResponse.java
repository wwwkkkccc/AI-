package com.resumeai.dto;

import java.util.ArrayList;
import java.util.List;

public class AnalyzeResponse {
    private Long analysisId;
    private Double score;
    private Double coverage;
    private List<String> matchedKeywords = new ArrayList<>();
    private List<String> missingKeywords = new ArrayList<>();
    private Boolean modelUsed;
    private OptimizedBlock optimized;
    private String optimizedMarkdown;

    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Long analysisId) {
        this.analysisId = analysisId;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getCoverage() {
        return coverage;
    }

    public void setCoverage(Double coverage) {
        this.coverage = coverage;
    }

    public List<String> getMatchedKeywords() {
        return matchedKeywords;
    }

    public void setMatchedKeywords(List<String> matchedKeywords) {
        this.matchedKeywords = matchedKeywords;
    }

    public List<String> getMissingKeywords() {
        return missingKeywords;
    }

    public void setMissingKeywords(List<String> missingKeywords) {
        this.missingKeywords = missingKeywords;
    }

    public Boolean getModelUsed() {
        return modelUsed;
    }

    public void setModelUsed(Boolean modelUsed) {
        this.modelUsed = modelUsed;
    }

    public OptimizedBlock getOptimized() {
        return optimized;
    }

    public void setOptimized(OptimizedBlock optimized) {
        this.optimized = optimized;
    }

    public String getOptimizedMarkdown() {
        return optimizedMarkdown;
    }

    public void setOptimizedMarkdown(String optimizedMarkdown) {
        this.optimizedMarkdown = optimizedMarkdown;
    }
}
