package com.lcj.campusreco;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@EnabledIfSystemProperty(named = "local.integration.enabled", matches = "true")
class LocalMysqlFlowIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldRunMainApiFlowAgainstLocalMysql() throws Exception {
        mockMvc.perform(post("/api/admin/mock/init"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userCount").value(18));

        MvcResult recommendationResult = mockMvc.perform(get("/api/recommendations/{userId}", 2001L)
                        .param("topK", "3")
                        .param("useCache", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].recommendationId").isString())
                .andExpect(jsonPath("$.data.items[0].targetUserId").isNumber())
                .andReturn();

        String recommendationBody = recommendationResult.getResponse().getContentAsString();
        String recommendationId = JsonPath.read(recommendationBody, "$.data.items[0].recommendationId");
        Number targetUserIdNumber = JsonPath.read(recommendationBody, "$.data.items[0].targetUserId");
        long targetUserId = targetUserIdNumber.longValue();

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
    }
}
