package com.resumeai.dto;

import java.time.Instant;

/**
 * 管理员配置查询响应 DTO，用于返回当前系统 LLM 配置信息
 */
public class AdminConfigResponse {
    /** LLM 服务的基础 URL */
    private String baseUrl;
    /** LLM 服务的 API 密钥 */
    private String apiKey;
    /** LLM 模型名称 */
    private String model;
    /** 配置最后更新时间 */
    private Instant updatedAt;

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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
