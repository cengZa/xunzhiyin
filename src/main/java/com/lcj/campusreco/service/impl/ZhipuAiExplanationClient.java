package com.lcj.campusreco.service.impl;

import com.lcj.campusreco.common.util.JsonUtils;
import com.lcj.campusreco.service.AiExplanationClient;
import com.lcj.campusreco.service.AiExplanationRequest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

@Service
public class ZhipuAiExplanationClient implements AiExplanationClient {

    private static final Logger log = LoggerFactory.getLogger(ZhipuAiExplanationClient.class);

    private final HttpClient httpClient;
    private final boolean enabled;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final Duration requestTimeout;

    public ZhipuAiExplanationClient(
            @Value("${app.ai.explanation.enabled:true}") boolean enabled,
            @Value("${app.ai.zhipu.base-url:https://open.bigmodel.cn/api/paas/v4}") String baseUrl,
            @Value("${app.ai.zhipu.api-key:${ZAI_API_KEY:}}") String apiKey,
            @Value("${app.ai.zhipu.model:GLM-4.7}") String model,
            @Value("${app.ai.zhipu.temperature:0.2}") double temperature,
            @Value("${app.ai.zhipu.timeout-ms:5000}") long timeoutMs) {
        this.enabled = enabled;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.requestTimeout = Duration.ofMillis(timeoutMs);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(this.requestTimeout)
                .build();
    }

    @Override
    public String generateExplanation(AiExplanationRequest request) {
        if (!enabled || apiKey == null || apiKey.isBlank() || request == null) {
            return null;
        }
        String responseBody = executeRequest(request);
        return extractContent(responseBody);
    }

    private String executeRequest(AiExplanationRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtils.toJson(buildPayload(request)), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Zhipu API returned status " + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Zhipu API call interrupted", ex);
        } catch (Exception ex) {
            log.warn("Failed to call Zhipu explanation API: {}", ex.getMessage());
            throw new IllegalStateException("Zhipu API call failed", ex);
        }
    }

    private Map<String, Object> buildPayload(AiExplanationRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", temperature);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", buildSystemPrompt()),
                Map.of("role", "user", "content", buildUserPrompt(request))
        ));
        return payload;
    }

    private String buildSystemPrompt() {
        return """
                你是校园匹配推荐系统的解释助手。
                你的任务是把结构化推荐证据改写成自然、准确、简洁的中文说明。
                约束：
                1. 只能使用输入中提供的事实，不能臆造任何用户信息。
                2. 只输出一段中文解释，不要输出 JSON，不要输出标题。
                3. 解释要覆盖：当前场景模式、关键匹配标签、命中规则或可信信号、探索位信息（如果存在）。
                4. 语气直接，适合本科毕业设计答辩展示。
                """;
    }

    private String buildUserPrompt(AiExplanationRequest request) {
        return """
                请基于以下推荐证据，生成一段 60 到 120 字的中文解释。

                recommendationId: %s
                scenarioMode: %s
                规则解释: %s
                evidenceJson: %s
                contributionJson: %s
                """.formatted(
                request.getRecommendationId(),
                nullToDash(request.getScenarioMode()),
                nullToDash(request.getRuleReasonText()),
                nullToDash(request.getEvidenceJson()),
                nullToDash(request.getContributionJson())
        );
    }

    private String extractContent(String responseBody) {
        Map<String, Object> root = JsonUtils.fromJson(responseBody, new TypeReference<Map<String, Object>>() {});
        Object choicesRaw = root.get("choices");
        if (!(choicesRaw instanceof List<?> choices) || choices.isEmpty()) {
            return null;
        }
        Object firstChoice = choices.getFirst();
        if (!(firstChoice instanceof Map<?, ?> choice)) {
            return null;
        }
        Object messageRaw = choice.get("message");
        if (!(messageRaw instanceof Map<?, ?> message)) {
            return null;
        }
        Object content = message.get("content");
        if (content instanceof String text) {
            return normalizeText(text);
        }
        if (content instanceof List<?> parts) {
            return normalizeText(parts.stream()
                    .map(this::extractContentPart)
                    .filter(part -> part != null && !part.isBlank())
                    .reduce("", (left, right) -> left.isEmpty() ? right : left + right));
        }
        return null;
    }

    private String extractContentPart(Object part) {
        if (part instanceof String text) {
            return text;
        }
        if (part instanceof Map<?, ?> partMap) {
            Object text = partMap.get("text");
            if (text instanceof String stringText) {
                return stringText;
            }
            Object content = partMap.get("content");
            if (content instanceof String stringContent) {
                return stringContent;
            }
        }
        return null;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://open.bigmodel.cn/api/paas/v4";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
