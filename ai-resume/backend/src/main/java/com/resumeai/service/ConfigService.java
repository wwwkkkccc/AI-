package com.resumeai.service;

import com.resumeai.dto.AdminConfigRequest;
import com.resumeai.dto.AdminConfigResponse;
import com.resumeai.model.AiConfig;
import com.resumeai.repository.AiConfigRepository;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 配置管理服务。
 * 负责维护全局唯一的 AI 调用配置（baseUrl、apiKey、model），
 * 采用单例行模式存储在数据库中，首次访问时自动用默认值初始化。
 */
@Service
public class ConfigService {
    // 配置表中唯一记录的固定 ID
    private static final long SINGLETON_ID = 1L;

    private final AiConfigRepository configRepository;
    private final String defaultBaseUrl;
    private final String defaultApiKey;
    private final String defaultModel;

    public ConfigService(
            AiConfigRepository configRepository,
            @Value("${app.ai.default-base-url:https://aicodelink.shop}") String defaultBaseUrl,
            @Value("${app.ai.default-api-key:}") String defaultApiKey,
            @Value("${app.ai.default-model:gpt-5.3-codex}") String defaultModel) {
        this.configRepository = configRepository;
        this.defaultBaseUrl = clean(defaultBaseUrl);
        this.defaultApiKey = clean(defaultApiKey);
        this.defaultModel = clean(defaultModel);
    }

    /**
     * 获取配置实体，若数据库中不存在则用默认值创建并持久化。
     * 使用 synchronized 保证并发安全，避免重复插入。
     */
    @Transactional
    public synchronized AiConfig getEntity() {
        return configRepository.findById(SINGLETON_ID).orElseGet(() -> {
            AiConfig cfg = new AiConfig();
            cfg.setId(SINGLETON_ID);
            cfg.setBaseUrl(defaultBaseUrl);
            cfg.setApiKey(defaultApiKey);
            cfg.setModel(defaultModel);
            cfg.setUpdatedAt(Instant.now());
            return configRepository.save(cfg);
        });
    }

    /** 查询当前 AI 配置并转为响应 DTO */
    @Transactional(readOnly = true)
    public AdminConfigResponse getConfig() {
        AiConfig cfg = getEntity();
        return toResponse(cfg);
    }

    /**
     * 更新 AI 配置。仅更新请求中非 null 的字段，
     * 若 model 被清空则回退到默认模型。
     */
    @Transactional
    public synchronized AdminConfigResponse updateConfig(AdminConfigRequest req) {
        AiConfig cfg = getEntity();
        if (req.getBaseUrl() != null) {
            cfg.setBaseUrl(clean(req.getBaseUrl()));
        }
        if (req.getApiKey() != null) {
            cfg.setApiKey(clean(req.getApiKey()));
        }
        if (req.getModel() != null) {
            String nextModel = clean(req.getModel());
            // 模型为空时回退到默认模型
            cfg.setModel(nextModel.isEmpty() ? defaultModel : nextModel);
        }
        cfg.setUpdatedAt(Instant.now());
        cfg = configRepository.save(cfg);
        return toResponse(cfg);
    }

    /** 将配置实体转换为管理端响应 DTO */
    private AdminConfigResponse toResponse(AiConfig cfg) {
        AdminConfigResponse resp = new AdminConfigResponse();
        resp.setBaseUrl(clean(cfg.getBaseUrl()));
        resp.setApiKey(clean(cfg.getApiKey()));
        resp.setModel(clean(cfg.getModel()));
        resp.setUpdatedAt(cfg.getUpdatedAt());
        return resp;
    }

    /** 清理字符串：null 转空串并去除首尾空白 */
    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
