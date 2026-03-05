package com.resumeai.dto;

import java.util.ArrayList;
import java.util.List;

public class OptimizedBlock {
    private String summary;
    private List<String> rewrittenExperience = new ArrayList<>();
    private List<String> skillsRecommendations = new ArrayList<>();
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
