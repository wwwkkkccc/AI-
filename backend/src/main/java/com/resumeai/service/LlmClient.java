package com.resumeai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.model.AiConfig;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Shared LLM client for OpenAI-compatible chat completion APIs.
 */
@Component
public class LlmClient {

    private final ConfigService configService;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    public LlmClient(ConfigService configService, ObjectMapper objectMapper, WebClient.Builder webClientBuilder) {
        this.configService = configService;
        this.objectMapper = objectMapper;
        this.webClientBuilder = webClientBuilder;
    }

    public String chat(List<Map<String, String>> messages, double temperature, int timeoutSeconds) {
        AiConfig cfg = configService.getEntity();
        String apiKey = clean(cfg.getApiKey());
        String model = clean(cfg.getModel());
        if (apiKey.isEmpty() || model.isEmpty()) {
            return null;
        }
        return chatWithConfig(
                messages,
                temperature,
                timeoutSeconds,
                normalizeBaseUrl(cfg.getBaseUrl()),
                apiKey,
                model
        );
    }

    public String chatWithConfig(
            List<Map<String, String>> messages,
            double temperature,
            int timeoutSeconds,
            String baseUrl,
            String apiKey,
            String model) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", temperature);
        payload.put("messages", messages);

        try {
            WebClient client = webClientBuilder
                    .baseUrl(normalizeBaseUrl(baseUrl))
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .build();

            JsonNode resp = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(timeoutSeconds));
            if (resp == null) {
                return null;
            }
            String content = resp.path("choices").path(0).path("message").path("content").asText("");
            return content.isBlank() ? null : content.trim();
        } catch (Exception ex) {
            return null;
        }
    }

    public JsonNode parseJsonObject(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            String trimmed = content.trim();
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                trimmed = trimmed.substring(start, end + 1);
            }
            return objectMapper.readTree(trimmed);
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String url = clean(baseUrl);
        if (url.isEmpty()) {
            return "https://api.openai.com/v1";
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.matches("^https?://[^/]+$")) {
            return url + "/v1";
        }
        return url;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
