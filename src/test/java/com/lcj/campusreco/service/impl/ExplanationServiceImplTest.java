package com.lcj.campusreco.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lcj.campusreco.domain.entity.RecommendationExplanationEntity;
import com.lcj.campusreco.domain.entity.RecommendationResultEntity;
import com.lcj.campusreco.infra.repository.RecommendationQueryRepository;
import com.lcj.campusreco.mapper.RecommendationExplanationMapper;
import com.lcj.campusreco.service.AiExplanationClient;
import com.lcj.campusreco.strategy.explain.ExplanationEvidenceExtractor;
import com.lcj.campusreco.strategy.explain.ExplanationTemplateBuilder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExplanationServiceImplTest {

    @Mock
    private RecommendationQueryRepository recommendationQueryRepository;
    @Mock
    private RecommendationExplanationMapper recommendationExplanationMapper;
    @Mock
    private AiExplanationClient aiExplanationClient;
    @Mock
    private ExplanationTemplateBuilder explanationTemplateBuilder;
    @Mock
    private ExplanationEvidenceExtractor explanationEvidenceExtractor;

    @InjectMocks
    private ExplanationServiceImpl explanationService;

    @Test
    void getByRecommendationIdReturnsReadableFallbackWhenExplanationMissing() {
        RecommendationResultEntity resultEntity = new RecommendationResultEntity();
        resultEntity.setId(101L);
        resultEntity.setTargetUserId(2002L);
        resultEntity.setCreatedAt(LocalDateTime.now());

        when(recommendationQueryRepository.getExplanationByRecommendationId(101L)).thenReturn(null);
        when(recommendationQueryRepository.getRecommendationResultById(101L)).thenReturn(resultEntity);

        var explanation = explanationService.getByRecommendationId(101L);

        assertEquals("101", explanation.getRecommendationId());
        assertEquals("该推荐记录暂未生成解释详情。", explanation.getReasonText());
        assertEquals("rule", explanation.getReasonSource());
        assertEquals("{}", explanation.getEvidenceJson());
        assertEquals("[]", explanation.getContributionJson());
        assertTrue(((Map<?, ?>) explanation.getEvidence()).isEmpty());
        assertTrue(((List<?>) explanation.getContribution()).isEmpty());
    }

    @Test
    void getByRecommendationIdReturnsLlmReasonWhenAvailable() {
        RecommendationExplanationEntity entity = new RecommendationExplanationEntity();
        entity.setRecommendationId(102L);
        entity.setReasonText("规则解释");
        entity.setEvidenceJson("{\"sharedTags\":[\"羽毛球\"]}");
        entity.setContributionJson("[{\"tag\":\"羽毛球\"}]");

        when(recommendationQueryRepository.getExplanationByRecommendationId(102L)).thenReturn(entity);
        when(aiExplanationClient.generateExplanation(any())).thenReturn("LLM 改写解释");

        var explanation = explanationService.getByRecommendationId(102L);

        assertEquals("LLM 改写解释", explanation.getReasonText());
        assertEquals("规则解释", explanation.getRuleReasonText());
        assertEquals("LLM 改写解释", explanation.getLlmReasonText());
        assertEquals("llm", explanation.getReasonSource());
        assertEquals("{\"sharedTags\":[\"羽毛球\"]}", explanation.getEvidenceJson());
        assertEquals("[{\"tag\":\"羽毛球\"}]", explanation.getContributionJson());
        assertInstanceOf(Map.class, explanation.getEvidence());
        assertInstanceOf(List.class, explanation.getContribution());
        assertEquals("羽毛球", ((List<?>) ((Map<?, ?>) explanation.getEvidence()).get("sharedTags")).getFirst());
        assertEquals("羽毛球", ((Map<?, ?>) ((List<?>) explanation.getContribution()).getFirst()).get("tag"));
    }

    @Test
    void getByRecommendationIdFallsBackToRuleReasonWhenLlmFails() {
        RecommendationExplanationEntity entity = new RecommendationExplanationEntity();
        entity.setRecommendationId(103L);
        entity.setReasonText("规则解释");
        entity.setEvidenceJson("{\"sharedTags\":[\"Java\"]}");
        entity.setContributionJson("[{\"tag\":\"Java\"}]");

        when(recommendationQueryRepository.getExplanationByRecommendationId(103L)).thenReturn(entity);
        when(aiExplanationClient.generateExplanation(any())).thenThrow(new RuntimeException("timeout"));

        var explanation = explanationService.getByRecommendationId(103L);

        assertEquals("规则解释", explanation.getReasonText());
        assertEquals("规则解释", explanation.getRuleReasonText());
        assertEquals("rule", explanation.getReasonSource());
    }

    @Test
    void getByRecommendationIdReusesLlmReasonForSameRuleExplanation() {
        RecommendationExplanationEntity first = new RecommendationExplanationEntity();
        first.setRecommendationId(104L);
        first.setReasonText("规则解释");
        first.setEvidenceJson("{\"scenarioMode\":\"interest_partner\"}");
        first.setContributionJson("[]");

        RecommendationExplanationEntity second = new RecommendationExplanationEntity();
        second.setRecommendationId(105L);
        second.setReasonText("规则解释");
        second.setEvidenceJson("{\"scenarioMode\":\"interest_partner\"}");
        second.setContributionJson("[]");

        when(recommendationQueryRepository.getExplanationByRecommendationId(104L)).thenReturn(first);
        when(recommendationQueryRepository.getExplanationByRecommendationId(105L)).thenReturn(second);
        when(aiExplanationClient.generateExplanation(any())).thenReturn("LLM 改写解释");

        var firstExplanation = explanationService.getByRecommendationId(104L);
        var secondExplanation = explanationService.getByRecommendationId(105L);

        assertEquals("LLM 改写解释", firstExplanation.getReasonText());
        assertEquals("LLM 改写解释", secondExplanation.getReasonText());
        assertEquals("llm", secondExplanation.getReasonSource());
        verify(aiExplanationClient, times(1)).generateExplanation(any());
    }
}
