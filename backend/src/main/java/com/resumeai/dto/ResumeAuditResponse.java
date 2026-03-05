package com.resumeai.dto;

import java.util.ArrayList;
import java.util.List;

public class ResumeAuditResponse {

    private String riskLevel;
    private Double riskScore;
    private String summary;
    private List<ResumeAuditItem> auditItems = new ArrayList<>();

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<ResumeAuditItem> getAuditItems() {
        return auditItems;
    }

    public void setAuditItems(List<ResumeAuditItem> auditItems) {
        this.auditItems = auditItems;
    }
}
