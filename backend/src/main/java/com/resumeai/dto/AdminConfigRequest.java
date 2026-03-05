package com.resumeai.dto;

import jakarta.validation.constraints.Size;

/**
 * 管理员配置更新请求 DTO，用于管理员修改系统 LLM 配置的接口请求体
 */
public class AdminConfigRequest {
    /** LLM 服务的基础 URL */
    @Size(max = 255)
    private String baseUrl;

    /** LLM 服务的 API 密钥 */
    @Size(max = 1024)
    private String apiKey;

    /** LLM 模型名称 */
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
