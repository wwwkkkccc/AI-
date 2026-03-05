package com.resumeai.service;

import com.resumeai.dto.AdminConfigRequest;
import com.resumeai.dto.AdminConfigResponse;
import com.resumeai.model.AiConfig;
import com.resumeai.repository.AiConfigRepository;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfigService {
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

    @Transactional(readOnly = true)
    public AdminConfigResponse getConfig() {
        AiConfig cfg = getEntity();
        return toResponse(cfg);
    }

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
            cfg.setModel(nextModel.isEmpty() ? defaultModel : nextModel);
        }
        cfg.setUpdatedAt(Instant.now());
        cfg = configRepository.save(cfg);
        return toResponse(cfg);
    }

    private AdminConfigResponse toResponse(AiConfig cfg) {
        AdminConfigResponse resp = new AdminConfigResponse();
        resp.setBaseUrl(clean(cfg.getBaseUrl()));
        resp.setApiKey(clean(cfg.getApiKey()));
        resp.setModel(clean(cfg.getModel()));
        resp.setUpdatedAt(cfg.getUpdatedAt());
        return resp;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
