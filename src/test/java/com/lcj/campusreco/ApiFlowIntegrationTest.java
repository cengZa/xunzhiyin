package com.lcj.campusreco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.Matchers;
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
    void shouldServeFrontendDashboard() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        MvcResult indexResult = mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andReturn();

        String html = indexResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("CampusReco 校园匹配首页");
        assertThat(html).contains("演示故事线");
        assertThat(html).contains("标签重叠基线 vs 完整链路");
        assertThat(html).contains("反馈前后变化");
        assertThat(html).contains("演示侧栏");

        MvcResult pipelineResult = mockMvc.perform(get("/pipeline.html"))
                .andExpect(status().isOk())
                .andReturn();
        String pipelineHtml = pipelineResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(pipelineHtml).contains("CampusReco 透明链路页");
        assertThat(pipelineHtml).contains("单个用户的推荐全链路");
    }

    @Test
    void shouldRunScenarioAwareRecommendationFlow() throws Exception {
        mockMvc.perform(post("/api/admin/mock/init"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tagCount").value(18))
                .andExpect(jsonPath("$.data.userCount").value(18))
                .andExpect(jsonPath("$.data.relationCount").value(86));

        mockMvc.perform(get("/api/admin/demo/story")
                        .param("scenarioMode", "study_partner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.demoUserId").value(2001))
                .andExpect(jsonPath("$.data.scenarioMode").value("study_partner"))
                .andExpect(jsonPath("$.data.scenarioLabel").value("学习搭子"));

        mockMvc.perform(get("/api/admin/demo/compare")
                        .param("userId", "2001")
                        .param("topK", "3")
                        .param("scenarioMode", "study_partner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(2001))
                .andExpect(jsonPath("$.data.scenarioMode").value("study_partner"))
                .andExpect(jsonPath("$.data.tagOverlapView.viewCode").value("tag_overlap"))
                .andExpect(jsonPath("$.data.fullPipelineView.viewCode").value("full_pipeline"));

        mockMvc.perform(get("/api/admin/demo/compare")
                        .param("userId", "2001")
                        .param("topK", "3")
                        .param("scenarioMode", "interest_partner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scenarioMode").value("interest_partner"))
                .andExpect(jsonPath("$.data.fullPipelineView.items[2].exploration").value(true))
                .andExpect(jsonPath("$.data.fullPipelineView.items[2].explorationReason").isNotEmpty());

        mockMvc.perform(get("/api/admin/demo/pipeline")
                        .param("userId", "2001")
                        .param("topK", "3")
                        .param("scenarioMode", "interest_partner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(2001))
                .andExpect(jsonPath("$.data.scenarioMode").value("interest_partner"))
                .andExpect(jsonPath("$.data.scenarioStage.objective").isNotEmpty())
                .andExpect(jsonPath("$.data.inputTags").isArray())
                .andExpect(jsonPath("$.data.inputTags[0].tagTypeLabel").isNotEmpty())
                .andExpect(jsonPath("$.data.profileStage").exists())
                .andExpect(jsonPath("$.data.profileStage.weightFormula").isNotEmpty())
                .andExpect(jsonPath("$.data.recallStage").isArray())
                .andExpect(jsonPath("$.data.recallStage[0].recallFormulaLabel").value("重叠召回标签数"))
                .andExpect(jsonPath("$.data.rankingStage").isArray())
                .andExpect(jsonPath("$.data.rankingStage[0].rankingFormulaLabel").value("余弦相似度"))
                .andExpect(jsonPath("$.data.rankingStage[0].interestScore").isNumber())
                .andExpect(jsonPath("$.data.rerankStage").isArray())
                .andExpect(jsonPath("$.data.rerankStage[0].trustBreakdown").exists())
                .andExpect(jsonPath("$.data.rerankStage[0].ruleDetails").isArray())
                .andExpect(jsonPath("$.data.finalStage").isArray())
                .andExpect(jsonPath("$.data.finalStage[2].exploration").value(true));

        mockMvc.perform(get("/api/admin/evaluation/summary")
                        .param("topK", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.topK").value(3))
                .andExpect(jsonPath("$.data.scenarioMode").isNotEmpty())
                .andExpect(jsonPath("$.data.baselines.length()").value(5));

        mockMvc.perform(post("/api/admin/evaluation/experiments/export")
                        .param("topKs", "3,5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").value("recommendation-evaluation-matrix-latest.md"))
                .andExpect(jsonPath("$.data.experimentCount").value(2));
        assertThat(Files.exists(Path.of("target/integration-generated-docs/recommendation-evaluation-matrix-latest.md"))).isTrue();

        mockMvc.perform(post("/api/admin/evaluation/scenarios/export")
                        .param("scenarioModes", "interest_partner,study_partner")
                        .param("topKs", "3,5")
                        .param("profileTopTagCounts", "3,5")
                        .param("rerankWeightScales", "0.8,1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").value("recommendation-scenario-matrix-latest.md"))
                .andExpect(jsonPath("$.data.scenarioCount").value(16))
                .andExpect(jsonPath("$.data.scenarioModes.length()").value(2));
        assertThat(Files.exists(Path.of("target/integration-generated-docs/recommendation-scenario-matrix-latest.md"))).isTrue();

        MvcResult recommendationResult = mockMvc.perform(get("/api/recommendations/{userId}", 2001L)
                        .param("topK", "3")
                        .param("useCache", "false")
                        .param("scenarioMode", "study_partner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requestTraceId").isNotEmpty())
                .andExpect(jsonPath("$.data.scenarioMode").value("study_partner"))
                .andExpect(jsonPath("$.data.scenarioLabel").value("学习搭子"))
                .andExpect(jsonPath("$.data.items[0].scenarioMode").value("study_partner"))
                .andExpect(jsonPath("$.data.items[0].scenarioLabel").value("学习搭子"))
                .andExpect(jsonPath("$.data.items[0].recommendationId").isString())
                .andExpect(jsonPath("$.data.items[0].targetUserId").isNumber())
                .andExpect(jsonPath("$.data.items[0].interestScore").isNumber())
                .andExpect(jsonPath("$.data.items[0].campusScore").isNumber())
                .andExpect(jsonPath("$.data.items[0].trustScore").isNumber())
                .andExpect(jsonPath("$.data.items[0].trustReasons").isArray())
                .andReturn();

        mockMvc.perform(get("/api/recommendations/{userId}", 2001L)
                        .param("topK", "3")
                        .param("useCache", "false")
                        .param("scenarioMode", "interest_partner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scenarioMode").value("interest_partner"))
                .andExpect(jsonPath("$.data.items[2].exploration").value(true))
                .andExpect(jsonPath("$.data.items[2].explorationScore").isNumber())
                .andExpect(jsonPath("$.data.items[2].explorationReason").isNotEmpty());

        String recommendationBody = recommendationResult.getResponse().getContentAsString();
        assertThat(recommendationBody).doesNotContain(":null");
        String recommendationId = JsonPath.read(recommendationBody, "$.data.items[0].recommendationId");
        Number targetUserIdNumber = JsonPath.read(recommendationBody, "$.data.items[0].targetUserId");
        long targetUserId = targetUserIdNumber.longValue();

        MvcResult explanationResult = mockMvc.perform(get("/api/recommendations/{recommendationId}/explanation", recommendationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recommendationId").value(recommendationId))
                .andExpect(jsonPath("$.data.reasonText").isNotEmpty())
                .andExpect(jsonPath("$.data.evidence").exists())
                .andExpect(jsonPath("$.data.contribution").exists())
                .andReturn();
        assertThat(explanationResult.getResponse().getContentAsString()).doesNotContain(":null");

        mockMvc.perform(get("/api/recommendations/{userId}/detail", 2001L)
                        .param("scenarioMode", "study_partner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scenarioMode").value("study_partner"))
                .andExpect(jsonPath("$.data.items[0].trustReasons").isArray())
                .andExpect(jsonPath("$.data.rankingDetails").isArray())
                .andExpect(jsonPath("$.data.rerankRuleHits").isArray())
                .andExpect(jsonPath("$.data.explanationEvidence").isArray());

        mockMvc.perform(post("/api/recommendations/{userId}/feedback", 2001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recommendationId": "%s",
                                  "targetUserId": %d,
                                  "feedbackType": "follow"
                                }
                                """.formatted(recommendationId, targetUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profileUpdated").value(true));

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

        mockMvc.perform(get("/api/admin/evaluation/report")
                        .param("topK", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(Matchers.containsString("推荐评估摘要")));
    }
}
