package com.lcj.campusreco.service.impl;

import com.lcj.campusreco.common.util.JsonUtils;
import com.lcj.campusreco.domain.entity.RecommendationExplanationEntity;
import com.lcj.campusreco.domain.entity.RecommendationResultEntity;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.vo.ExplanationVO;
import com.lcj.campusreco.infra.repository.RecommendationQueryRepository;
import com.lcj.campusreco.mapper.RecommendationExplanationMapper;
import com.lcj.campusreco.service.AiExplanationClient;
import com.lcj.campusreco.service.AiExplanationRequest;
import com.lcj.campusreco.service.ExplanationService;
import com.lcj.campusreco.strategy.explain.ExplanationEvidenceExtractor;
import com.lcj.campusreco.strategy.explain.ExplanationTemplateBuilder;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

@Service
public class ExplanationServiceImpl implements ExplanationService {

    private static final String EMPTY_EVIDENCE_JSON = "{}";
    private static final String EMPTY_CONTRIBUTION_JSON = "[]";
    private static final String MISSING_RECOMMENDATION_MESSAGE = "未找到对应的推荐记录。";
    private static final String MISSING_EXPLANATION_MESSAGE = "该推荐记录暂未生成解释详情。";

    private final RecommendationQueryRepository recommendationQueryRepository;
    private final RecommendationExplanationMapper recommendationExplanationMapper;
    private final AiExplanationClient aiExplanationClient;
    private final ExplanationTemplateBuilder explanationTemplateBuilder;
    private final ExplanationEvidenceExtractor explanationEvidenceExtractor;
    private final Map<String, String> llmReasonCache = new ConcurrentHashMap<>();

    public ExplanationServiceImpl(RecommendationQueryRepository recommendationQueryRepository,
                                  RecommendationExplanationMapper recommendationExplanationMapper,
                                  AiExplanationClient aiExplanationClient,
                                  ExplanationTemplateBuilder explanationTemplateBuilder,
                                  ExplanationEvidenceExtractor explanationEvidenceExtractor) {
        this.recommendationQueryRepository = recommendationQueryRepository;
        this.recommendationExplanationMapper = recommendationExplanationMapper;
        this.aiExplanationClient = aiExplanationClient;
        this.explanationTemplateBuilder = explanationTemplateBuilder;
        this.explanationEvidenceExtractor = explanationEvidenceExtractor;
    }

    @Override
    public ExplanationVO generate(RankingCandidateModel candidate) {
        ExplanationVO explanationVO = new ExplanationVO();
        String ruleReasonText = explanationTemplateBuilder.build(candidate);
        Object evidence = explanationEvidenceExtractor.extract(candidate);
        Object contribution = candidate == null ? null : candidate.getContributions();
        explanationVO.setRecommendationId(null);
        explanationVO.setReasonText(ruleReasonText);
        explanationVO.setRuleReasonText(ruleReasonText);
        explanationVO.setReasonSource("rule");
        explanationVO.setEvidenceJson(evidence);
        explanationVO.setContributionJson(contribution);
        explanationVO.setEvidence(evidence);
        explanationVO.setContribution(contribution);
        return explanationVO;
    }

    @Override
    public void batchSaveExplanation(List<RankingCandidateModel> candidates, Map<Long, Long> recommendationIdMap) {
        for (RankingCandidateModel candidate : candidates) {
            Long recommendationId = recommendationIdMap.get(candidate.getTargetUserId());
            if (recommendationId == null) {
                continue;
            }
            ExplanationVO explanationVO = generate(candidate);
            RecommendationExplanationEntity entity = new RecommendationExplanationEntity();
            entity.setRecommendationId(recommendationId);
            entity.setReasonText(explanationVO.getReasonText());
            entity.setEvidenceJson(JsonUtils.toJson(explanationVO.getEvidenceJson()));
            entity.setContributionJson(JsonUtils.toJson(explanationVO.getContributionJson()));
            entity.setCreatedAt(LocalDateTime.now());
            recommendationExplanationMapper.insert(entity);
        }
    }

    @Override
    public ExplanationVO getByRecommendationId(Long recommendationId) {
        ExplanationVO explanationVO = new ExplanationVO();
        explanationVO.setRecommendationId(recommendationId == null ? null : String.valueOf(recommendationId));

        RecommendationExplanationEntity explanationEntity = recommendationQueryRepository.getExplanationByRecommendationId(recommendationId);
        if (explanationEntity != null) {
            return buildPersistedExplanation(explanationVO, explanationEntity);
        }

        RecommendationResultEntity recommendationResult = recommendationQueryRepository.getRecommendationResultById(recommendationId);
        return buildFallbackExplanation(explanationVO,
                recommendationResult == null ? MISSING_RECOMMENDATION_MESSAGE : MISSING_EXPLANATION_MESSAGE);
    }

    private ExplanationVO buildPersistedExplanation(ExplanationVO explanationVO, RecommendationExplanationEntity entity) {
        String ruleReasonText = defaultReasonText(entity.getReasonText());
        String evidenceJson = normalizeJsonText(entity.getEvidenceJson(), EMPTY_EVIDENCE_JSON);
        String contributionJson = normalizeJsonText(entity.getContributionJson(), EMPTY_CONTRIBUTION_JSON);
        Object evidence = parseEvidence(entity.getEvidenceJson());
        Object contribution = parseContribution(entity.getContributionJson());

        explanationVO.setReasonText(ruleReasonText);
        explanationVO.setRuleReasonText(ruleReasonText);
        explanationVO.setReasonSource("rule");
        explanationVO.setEvidenceJson(evidenceJson);
        explanationVO.setContributionJson(contributionJson);
        explanationVO.setEvidence(evidence);
        explanationVO.setContribution(contribution);

        String scenarioMode = extractScenarioMode(evidence);
        String cacheKey = buildLlmCacheKey(scenarioMode, ruleReasonText);
        String cachedLlmReasonText = llmReasonCache.get(cacheKey);
        if (cachedLlmReasonText != null && !cachedLlmReasonText.isBlank()) {
            applyLlmReason(explanationVO, cachedLlmReasonText);
            return explanationVO;
        }

        try {
            String llmReasonText = aiExplanationClient.generateExplanation(
                    AiExplanationRequest.builder()
                            .recommendationId(entity.getRecommendationId())
                            .scenarioMode(scenarioMode)
                            .ruleReasonText(ruleReasonText)
                            .evidence(evidence)
                            .contribution(contribution)
                            .evidenceJson(evidenceJson)
                            .contributionJson(contributionJson)
                            .build()
            );
            if (llmReasonText != null && !llmReasonText.isBlank()) {
                llmReasonCache.put(cacheKey, llmReasonText);
                applyLlmReason(explanationVO, llmReasonText);
            }
        } catch (Exception ignored) {
            explanationVO.setReasonText(ruleReasonText);
            explanationVO.setReasonSource("rule");
        }
        return explanationVO;
    }

    private void applyLlmReason(ExplanationVO explanationVO, String llmReasonText) {
        explanationVO.setReasonText(llmReasonText);
        explanationVO.setLlmReasonText(llmReasonText);
        explanationVO.setReasonSource("llm");
    }

    private String buildLlmCacheKey(String scenarioMode, String ruleReasonText) {
        return defaultReasonText(scenarioMode) + "|" + defaultReasonText(ruleReasonText);
    }

    private ExplanationVO buildFallbackExplanation(ExplanationVO explanationVO, String reasonText) {
        explanationVO.setReasonText(reasonText);
        explanationVO.setRuleReasonText(reasonText);
        explanationVO.setReasonSource("rule");
        explanationVO.setEvidenceJson(EMPTY_EVIDENCE_JSON);
        explanationVO.setContributionJson(EMPTY_CONTRIBUTION_JSON);
        explanationVO.setEvidence(Collections.emptyMap());
        explanationVO.setContribution(Collections.emptyList());
        return explanationVO;
    }

    private String defaultReasonText(String reasonText) {
        return reasonText == null || reasonText.isBlank() ? MISSING_EXPLANATION_MESSAGE : reasonText;
    }

    private String extractScenarioMode(Object evidence) {
        if (evidence instanceof Map<?, ?> evidenceMap) {
            Object scenarioMode = evidenceMap.get("scenarioMode");
            if (scenarioMode instanceof String mode && !mode.isBlank()) {
                return mode;
            }
        }
        return null;
    }

    private String normalizeJsonText(String raw, String fallback) {
        if (raw == null || raw.isBlank() || "null".equalsIgnoreCase(raw.trim())) {
            return fallback;
        }
        return raw;
    }

    private Object parseEvidence(String raw) {
        String normalized = normalizeJsonText(raw, EMPTY_EVIDENCE_JSON);
        try {
            return JsonUtils.fromJson(normalized, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("raw", normalized);
            return evidence;
        }
    }

    private Object parseContribution(String raw) {
        String normalized = normalizeJsonText(raw, EMPTY_CONTRIBUTION_JSON);
        try {
            return JsonUtils.fromJson(normalized, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ignored) {
            return List.of(normalized);
        }
    }
}
