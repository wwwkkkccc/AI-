package com.resumeai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class InterviewKbLlmImportRequest {
    @NotBlank(message = "llm_topic is required")
    @Size(max = 120)
    private String topic;

    @Size(max = 255)
    private String title;

    @NotBlank(message = "llm_base_url is required")
    @Size(max = 255)
    private String baseUrl;

    @NotBlank(message = "llm_api_key is required")
    @Size(max = 1024)
    private String apiKey;

    @Size(max = 128)
    private String model;

    @Min(value = 5, message = "question_count must be between 5 and 120")
    @Max(value = 120, message = "question_count must be between 5 and 120")
    private Integer questionCount;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

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

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }
}
