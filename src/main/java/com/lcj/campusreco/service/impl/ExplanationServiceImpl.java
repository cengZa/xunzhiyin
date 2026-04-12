package com.lcj.campusreco.service.impl;

import com.lcj.campusreco.common.util.JsonUtils;
import com.lcj.campusreco.domain.entity.RecommendationExplanationEntity;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.vo.ExplanationVO;
import com.lcj.campusreco.infra.repository.RecommendationQueryRepository;
import com.lcj.campusreco.mapper.RecommendationExplanationMapper;
import com.lcj.campusreco.service.ExplanationService;
import com.lcj.campusreco.strategy.explain.ExplanationEvidenceExtractor;
import com.lcj.campusreco.strategy.explain.ExplanationTemplateBuilder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ExplanationServiceImpl implements ExplanationService {

    private final RecommendationQueryRepository recommendationQueryRepository;
    private final RecommendationExplanationMapper recommendationExplanationMapper;
    private final ExplanationTemplateBuilder explanationTemplateBuilder;
    private final ExplanationEvidenceExtractor explanationEvidenceExtractor;

    public ExplanationServiceImpl(RecommendationQueryRepository recommendationQueryRepository,
                                  RecommendationExplanationMapper recommendationExplanationMapper,
                                  ExplanationTemplateBuilder explanationTemplateBuilder,
                                  ExplanationEvidenceExtractor explanationEvidenceExtractor) {
        this.recommendationQueryRepository = recommendationQueryRepository;
        this.recommendationExplanationMapper = recommendationExplanationMapper;
        this.explanationTemplateBuilder = explanationTemplateBuilder;
        this.explanationEvidenceExtractor = explanationEvidenceExtractor;
    }

    @Override
    public ExplanationVO generate(RankingCandidateModel candidate) {
        ExplanationVO explanationVO = new ExplanationVO();
        explanationVO.setRecommendationId(null);
        explanationVO.setReasonText(explanationTemplateBuilder.build(candidate));
        Object evidence = explanationEvidenceExtractor.extract(candidate);
        Object contribution = candidate == null ? null : candidate.getContributions();
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
        var explanationEntity = recommendationQueryRepository.getExplanationByRecommendationId(recommendationId);
        ExplanationVO explanationVO = new ExplanationVO();
        explanationVO.setRecommendationId(recommendationId);
        if (explanationEntity != null) {
            explanationVO.setReasonText(explanationEntity.getReasonText());
            explanationVO.setEvidenceJson(explanationEntity.getEvidenceJson());
            explanationVO.setContributionJson(explanationEntity.getContributionJson());
            explanationVO.setEvidence(explanationEntity.getEvidenceJson());
            explanationVO.setContribution(explanationEntity.getContributionJson());
        }
        return explanationVO;
    }
}
