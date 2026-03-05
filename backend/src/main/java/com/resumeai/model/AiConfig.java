package com.resumeai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * AI配置表，存储AI服务的接口地址、密钥和模型等配置信息
 */
@Entity
@Table(name = "ai_config")
public class AiConfig {
    // 主键ID
    @Id
    private Long id;

    // AI服务的基础URL地址
    @Column(name = "base_url", length = 255)
    private String baseUrl;

    // AI服务的API密钥
    @Column(name = "api_key", length = 1024)
    private String apiKey;

    // 使用的AI模型名称
    @Column(name = "model", length = 128)
    private String model;

    // 配置最后更新时间
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
