package com.lcj.campusreco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("integration")
class ApiFlowIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldRunMockInitRecommendFeedbackAndProfileFlow() throws Exception {
        mockMvc.perform(post("/api/admin/mock/init"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tagCount").value(12))
                .andExpect(jsonPath("$.data.userCount").value(12))
                .andExpect(jsonPath("$.data.relationCount").value(48));

        mockMvc.perform(get("/api/admin/evaluation/summary")
                        .param("topK", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.topK").value(3))
                .andExpect(jsonPath("$.data.activeUserCount").value(12))
                .andExpect(jsonPath("$.data.baselines.length()").value(3))
                .andExpect(jsonPath("$.data.baselines[0].baselineCode").isNotEmpty())
                .andExpect(jsonPath("$.data.baselines[0].precisionAtK").isNumber());

        mockMvc.perform(get("/api/admin/evaluation/report")
                        .param("topK", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isString())
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("Recommendation Evaluation Summary")))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("Precision@K")));

        mockMvc.perform(post("/api/admin/evaluation/export")
                        .param("topK", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").value("recommendation-evaluation-latest.md"))
                .andExpect(jsonPath("$.data.topK").value(3))
                .andExpect(jsonPath("$.data.baselineCount").value(3));
        assertThat(Files.exists(Path.of("target/generated-docs/recommendation-evaluation-latest.md"))).isTrue();

        mockMvc.perform(post("/api/admin/evaluation/experiments/export")
                        .param("topKs", "3,5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").value("recommendation-evaluation-matrix-latest.md"))
                .andExpect(jsonPath("$.data.experimentCount").value(2))
                .andExpect(jsonPath("$.data.topKValues.length()").value(2));
        assertThat(Files.exists(Path.of("target/generated-docs/recommendation-evaluation-matrix-latest.md"))).isTrue();

        MvcResult recommendationResult = mockMvc.perform(get("/api/recommendations/{userId}", 2001L)
                        .param("topK", "3")
                        .param("useCache", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requestTraceId").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].recommendationId").isNumber())
                .andExpect(jsonPath("$.data.items[0].targetUserId").isNumber())
                .andExpect(jsonPath("$.data.items[0].finalScore").isNumber())
                .andExpect(jsonPath("$.data.items[0].explanation").isNotEmpty())
                .andReturn();

        String recommendationBody = recommendationResult.getResponse().getContentAsString();
        Number recommendationIdNumber = JsonPath.read(recommendationBody, "$.data.items[0].recommendationId");
        Number targetUserIdNumber = JsonPath.read(recommendationBody, "$.data.items[0].targetUserId");
        long recommendationId = recommendationIdNumber.longValue();
        long targetUserId = targetUserIdNumber.longValue();

        mockMvc.perform(get("/api/recommendations/{recommendationId}/explanation", recommendationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recommendationId").value(recommendationId))
                .andExpect(jsonPath("$.data.reasonText").isNotEmpty());

        mockMvc.perform(post("/api/recommendations/{userId}/feedback", 2001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recommendationId": %d,
                                  "targetUserId": %d,
                                  "feedbackType": "follow"
                                }
                                """.formatted(recommendationId, targetUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profileUpdated").value(true));

        MvcResult buildProfileResult = mockMvc.perform(post("/api/profiles/{userId}/build", 2001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profileVersion").isNumber())
                .andExpect(jsonPath("$.data.profileJson").isNotEmpty())
                .andExpect(jsonPath("$.data.topkJson").isNotEmpty())
                .andReturn();

        String buildProfileBody = buildProfileResult.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(buildProfileBody, "$.data.profileJson")).isNotEqualTo("{}");
        assertThat(JsonPath.<String>read(buildProfileBody, "$.data.topkJson")).isNotEqualTo("[]");

        MvcResult getProfileResult = mockMvc.perform(get("/api/profiles/{userId}", 2001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(2001))
                .andExpect(jsonPath("$.data.profileJson").isNotEmpty())
                .andExpect(jsonPath("$.data.topkJson").isNotEmpty())
                .andReturn();

        String getProfileBody = getProfileResult.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(getProfileBody, "$.data.profileJson")).isNotEqualTo("{}");
        assertThat(JsonPath.<String>read(getProfileBody, "$.data.topkJson")).isNotEqualTo("[]");
    }
}
