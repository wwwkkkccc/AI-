package com.resumeai.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 简历优化建议块 DTO，用于封装 LLM 生成的结构化简历优化内容
 */
public class OptimizedBlock {
    /** 优化后的简历摘要 */
    private String summary;
    /** 重写后的工作经历描述列表 */
    private List<String> rewrittenExperience = new ArrayList<>();
    /** 技能提升建议列表 */
    private List<String> skillsRecommendations = new ArrayList<>();
    /** 可能的面试问题列表 */
    private List<String> interviewQuestions = new ArrayList<>();

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getRewrittenExperience() {
        return rewrittenExperience;
    }

    public void setRewrittenExperience(List<String> rewrittenExperience) {
        this.rewrittenExperience = rewrittenExperience;
    }

    public List<String> getSkillsRecommendations() {
        return skillsRecommendations;
    }

    public void setSkillsRecommendations(List<String> skillsRecommendations) {
        this.skillsRecommendations = skillsRecommendations;
    }

    public List<String> getInterviewQuestions() {
        return interviewQuestions;
    }

    public void setInterviewQuestions(List<String> interviewQuestions) {
        this.interviewQuestions = interviewQuestions;
    }
}
