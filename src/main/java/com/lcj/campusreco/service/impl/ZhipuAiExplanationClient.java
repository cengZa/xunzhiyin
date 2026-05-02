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
    private final int maxTokens;
    private final Duration requestTimeout;

    public ZhipuAiExplanationClient(
            @Value("${app.ai.explanation.enabled:true}") boolean enabled,
            @Value("${app.ai.zhipu.base-url:https://open.bigmodel.cn/api/paas/v4}") String baseUrl,
            @Value("${app.ai.zhipu.api-key:${ZAI_API_KEY:}}") String apiKey,
            @Value("${app.ai.zhipu.model:glm-4-flash-250414}") String model,
            @Value("${app.ai.zhipu.temperature:0.2}") double temperature,
            @Value("${app.ai.zhipu.max-tokens:220}") int maxTokens,
            @Value("${app.ai.zhipu.timeout-ms:5000}") long timeoutMs) {
        this.enabled = enabled;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
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
        payload.put("max_tokens", maxTokens);
        payload.put("thinking", Map.of("type", "disabled"));
        payload.put("messages", List.of(
                Map.of("role", "system", "content", buildSystemPrompt()),
                Map.of("role", "user", "content", buildUserPrompt(request))
        ));
        return payload;
    }

    private String buildSystemPrompt() {
        return """
                你是校园匹配推荐系统的用户解释生成助手。
                你不参与召回、评分、排序或重排，只负责把系统已经生成的规则证据转换成用户可读解释。
                约束：
                1. 只能使用输入中提供的事实，不能臆造任何用户信息。
                2. 输出要明显区别于规则模板，不要机械复述“推荐原因”“命中了”“可信连接信号来自”等模板化措辞。
                3. 进行解释性转述：共同标签可说明为交流基础，场景规则可说明为校园情境更接近，可信信号可说明为信息支撑更充分。
                4. 用完整语句说明共同标签、场景规则、可信信号或探索位对用户理解推荐结果的意义。
                5. 采用客观书面表达，不要使用“我”“我们”“我觉得”等第一人称或拟人化说法。
                6. 只输出一段中文解释，不要输出 JSON，不要输出标题或编号。
                """;
    }

    private String buildUserPrompt(AiExplanationRequest request) {
        return """
                请基于以下规则解释生成 90 到 130 字的用户可读说明。
                写法要求：先给出推荐结论，再说明共同标签、场景规则和可信信号分别提供了什么依据。
                不要新增规则解释中没有出现的事实，不要改变推荐含义。
                不要使用“推荐原因：”“命中了”等模板化开头，也不要使用“我”“我们”“我觉得”等第一人称。
                尽量不要照抄规则解释中的长句，应把规则信号解释成用户能理解的推荐依据。

                recommendationId: %s
                scenarioMode: %s
                规则解释: %s
                """.formatted(
                request.getRecommendationId(),
                nullToDash(request.getScenarioMode()),
                nullToDash(request.getRuleReasonText())
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
