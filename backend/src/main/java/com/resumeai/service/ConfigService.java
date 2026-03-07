package com.resumeai.service;

import com.resumeai.model.AiConfig;
import com.resumeai.repository.AiConfigRepository;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides a singleton AI configuration row for all LLM calls.
 */
@Service
public class ConfigService {
    private static final long SINGLETON_ID = 1L;

    private final AiConfigRepository configRepository;
    private final String defaultBaseUrl;
    private final String defaultApiKey;
    private final String defaultModel;
    private volatile AiConfig cachedConfig;

    public ConfigService(
            AiConfigRepository configRepository,
            @Value("${app.ai.default-base-url:https://api.openai.com/v1}") String defaultBaseUrl,
            @Value("${app.ai.default-api-key:}") String defaultApiKey,
            @Value("${app.ai.default-model:gpt-5.3-codex}") String defaultModel) {
        this.configRepository = configRepository;
        this.defaultBaseUrl = clean(defaultBaseUrl);
        this.defaultApiKey = clean(defaultApiKey);
        this.defaultModel = clean(defaultModel);
    }

    @Transactional
    public synchronized AiConfig getEntity() {
        if (cachedConfig != null) {
            return cachedConfig;
        }
        cachedConfig = configRepository.findById(SINGLETON_ID).orElseGet(() -> {
            AiConfig cfg = new AiConfig();
            cfg.setId(SINGLETON_ID);
            cfg.setBaseUrl(defaultBaseUrl);
            cfg.setApiKey(defaultApiKey);
            cfg.setModel(defaultModel);
            cfg.setUpdatedAt(Instant.now());
            return configRepository.save(cfg);
        });
        return cachedConfig;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
