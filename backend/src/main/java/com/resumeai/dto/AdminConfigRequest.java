package com.resumeai.dto;

import jakarta.validation.constraints.Size;

public class AdminConfigRequest {
    @Size(max = 255)
    private String baseUrl;

    @Size(max = 1024)
    private String apiKey;

    @Size(max = 128)
    private String model;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
