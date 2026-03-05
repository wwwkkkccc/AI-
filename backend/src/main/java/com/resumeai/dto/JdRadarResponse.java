package com.resumeai.dto;

import java.util.ArrayList;
import java.util.List;

public class JdRadarResponse {

    private Double overallScore;
    private List<JdDimension> dimensions = new ArrayList<>();
    private List<String> extractedTechStack = new ArrayList<>();
    private List<String> extractedSoftSkills = new ArrayList<>();
    private String extractedYearsRequirement;

    public Double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Double overallScore) {
        this.overallScore = overallScore;
    }

    public List<JdDimension> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<JdDimension> dimensions) {
        this.dimensions = dimensions;
    }

    public List<String> getExtractedTechStack() {
        return extractedTechStack;
    }

    public void setExtractedTechStack(List<String> extractedTechStack) {
        this.extractedTechStack = extractedTechStack;
    }

    public List<String> getExtractedSoftSkills() {
        return extractedSoftSkills;
    }

    public void setExtractedSoftSkills(List<String> extractedSoftSkills) {
        this.extractedSoftSkills = extractedSoftSkills;
    }

    public String getExtractedYearsRequirement() {
        return extractedYearsRequirement;
    }

    public void setExtractedYearsRequirement(String extractedYearsRequirement) {
        this.extractedYearsRequirement = extractedYearsRequirement;
    }
}
