package com.resumeai.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 简历分析结果响应 DTO，用于返回简历与职位描述的匹配分析结果
 */
public class AnalyzeResponse {
    /** 分析记录 ID */
    private Long analysisId;
    /** 简历匹配评分 */
    private Double score;
    /** 关键词覆盖率 */
    private Double coverage;
    /** 简历中已匹配的关键词列表 */
    private List<String> matchedKeywords = new ArrayList<>();
    /** 简历中缺失的关键词列表 */
    private List<String> missingKeywords = new ArrayList<>();
    /** 是否使用了 LLM 模型进行分析 */
    private Boolean modelUsed;
    /** 结构化的优化建议 */
    private OptimizedBlock optimized;
    /** Markdown 格式的优化建议 */
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
