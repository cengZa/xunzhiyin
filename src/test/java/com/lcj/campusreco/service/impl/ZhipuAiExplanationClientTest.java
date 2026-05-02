package com.lcj.campusreco.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lcj.campusreco.common.util.JsonUtils;
import com.lcj.campusreco.service.AiExplanationRequest;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

class ZhipuAiExplanationClientTest {

    @Test
    void generateExplanationUsesShortNonThinkingChatRequest() throws IOException {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = """
                    {"choices":[{"message":{"content":"LLM 改写解释"}}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            ZhipuAiExplanationClient client = new ZhipuAiExplanationClient(
                    true,
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    "test-key",
                    "glm-4.7-flash",
                    0.2,
                    220,
                    3000
            );

            String result = client.generateExplanation(AiExplanationRequest.builder()
                    .recommendationId(1L)
                    .scenarioMode("interest_partner")
                    .ruleReasonText("规则解释")
                    .evidenceJson("{}")
                    .contributionJson("[]")
                    .build());

            assertEquals("LLM 改写解释", result);
            Map<String, Object> payload = JsonUtils.fromJson(
                    capturedBody.get(),
                    new TypeReference<Map<String, Object>>() {}
            );
            assertEquals("glm-4.7-flash", payload.get("model"));
            assertEquals(220, ((Number) payload.get("max_tokens")).intValue());
            assertEquals(Map.of("type", "disabled"), payload.get("thinking"));
            List<?> messages = (List<?>) payload.get("messages");
            String systemPrompt = (String) ((Map<?, ?>) messages.get(0)).get("content");
            String userPrompt = (String) ((Map<?, ?>) messages.get(1)).get("content");
            org.junit.jupiter.api.Assertions.assertTrue(systemPrompt.contains("不参与召回、评分、排序或重排"));
            org.junit.jupiter.api.Assertions.assertTrue(systemPrompt.contains("明显区别于规则模板"));
            org.junit.jupiter.api.Assertions.assertTrue(systemPrompt.contains("解释性转述"));
            org.junit.jupiter.api.Assertions.assertTrue(systemPrompt.contains("客观书面表达"));
            org.junit.jupiter.api.Assertions.assertTrue(userPrompt.contains("用户可读说明"));
            org.junit.jupiter.api.Assertions.assertTrue(userPrompt.contains("不要使用“推荐原因：”“命中了”等模板化开头"));
            org.junit.jupiter.api.Assertions.assertTrue(userPrompt.contains("不要使用“我”“我们”“我觉得”等第一人称"));
            org.junit.jupiter.api.Assertions.assertTrue(userPrompt.contains("不要照抄规则解释中的长句"));
        } finally {
            server.stop(0);
        }
    }
}
