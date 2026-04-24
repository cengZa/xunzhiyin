package com.lcj.campusreco.service.impl;

import com.lcj.campusreco.common.constant.RecommendationScenarioMode;
import com.lcj.campusreco.config.RecommendationTuningContext;
import com.lcj.campusreco.domain.dto.RecommendRequestDTO;
import com.lcj.campusreco.domain.entity.RecommendationResultEntity;
import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.UserProfileModel;
import com.lcj.campusreco.domain.vo.ExplanationVO;
import com.lcj.campusreco.domain.vo.RecommendationDetailVO;
import com.lcj.campusreco.domain.vo.RecommendationItemVO;
import com.lcj.campusreco.infra.repository.RecommendationQueryRepository;
import com.lcj.campusreco.mapper.RecommendationResultMapper;
import com.lcj.campusreco.service.ExplorationService;
import com.lcj.campusreco.service.ExplanationService;
import com.lcj.campusreco.service.ProfileService;
import com.lcj.campusreco.service.RankingService;
import com.lcj.campusreco.service.RecallService;
import com.lcj.campusreco.service.RecommendationService;
import com.lcj.campusreco.service.RerankService;
import com.lcj.campusreco.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final ProfileService profileService;
    private final RecallService recallService;
    private final RankingService rankingService;
    private final RerankService rerankService;
    private final ExplorationService explorationService;
    private final ExplanationService explanationService;
    private final UserService userService;
    private final RecommendationQueryRepository recommendationQueryRepository;
    private final RecommendationResultMapper recommendationResultMapper;
    private final RecommendationTuningContext tuningContext;

    public RecommendationServiceImpl(ProfileService profileService,
                                     RecallService recallService,
                                     RankingService rankingService,
                                     RerankService rerankService,
                                     ExplorationService explorationService,
                                     ExplanationService explanationService,
                                     UserService userService,
                                     RecommendationQueryRepository recommendationQueryRepository,
                                     RecommendationResultMapper recommendationResultMapper,
                                     RecommendationTuningContext tuningContext) {
        this.profileService = profileService;
        this.recallService = recallService;
        this.rankingService = rankingService;
        this.rerankService = rerankService;
        this.explorationService = explorationService;
        this.explanationService = explanationService;
        this.userService = userService;
        this.recommendationQueryRepository = recommendationQueryRepository;
        this.recommendationResultMapper = recommendationResultMapper;
        this.tuningContext = tuningContext;
    }

    @Override
    public RecommendationDetailVO recommend(RecommendRequestDTO dto) {
        String scenarioMode = RecommendationScenarioMode.normalize(dto.getScenarioMode());
        List<RankingCandidateModel> topList;
        int recallCandidatesCount;
        try (RecommendationTuningContext.Scope ignored =
                     tuningContext.withOverrides(null, null, scenarioMode, true)) {
            UserProfileModel profileModel = profileService.getProfile(dto.getUserId());
            if (profileModel.getVector().isEmpty()) {
                profileModel = profileService.buildProfile(dto.getUserId(), "init");
            }
            var candidateUserIds = recallService.recallCandidateUserIds(profileModel);
            recallCandidatesCount = candidateUserIds.size();
            List<RankingCandidateModel> rankingList = rankingService.rank(dto.getUserId(), candidateUserIds);
            List<RankingCandidateModel> rerankedList = rerankService.rerank(dto.getUserId(), rankingList);
            topList = explorationService.apply(dto.getUserId(), rerankedList, dto.getTopK(), scenarioMode);
        }

        RecommendationDetailVO detailVO = new RecommendationDetailVO();
        String traceId = UUID.randomUUID().toString();
        detailVO.setRequestTraceId(traceId);
        detailVO.setRecallCandidatesCount(recallCandidatesCount);
        detailVO.setRecallCandidateCount(recallCandidatesCount);
        detailVO.setScenarioMode(scenarioMode);
        detailVO.setScenarioLabel(RecommendationScenarioMode.labelOf(scenarioMode));

        Map<Long, Long> recommendationIdMap = saveRecommendationResults(dto.getUserId(), traceId, topList);
        explanationService.batchSaveExplanation(topList, recommendationIdMap);
        detailVO.setRankingDetails(buildRankingDetails(topList));
        detailVO.setRerankRuleHits(buildRerankRuleHits(topList));

        List<Map<String, Object>> explanationEvidence = new ArrayList<>();
        Map<Long, UserEntity> userCache = new HashMap<>();
        int rankNo = 1;
        for (RankingCandidateModel candidate : topList) {
            Long recommendationId = recommendationIdMap.get(candidate.getTargetUserId());
            ExplanationVO explanationVO = explanationService.generate(candidate);
            RecommendationItemVO itemVO = toRecommendationItem(
                    candidate,
                    explanationVO,
                    recommendationId,
                    resolveTargetNickname(candidate.getTargetUserId(), userCache),
                    rankNo++
            );
            detailVO.getItems().add(itemVO);
            explanationEvidence.add(buildExplanationEvidenceItem(recommendationId, candidate, explanationVO));
        }
        detailVO.setExplanationEvidence(explanationEvidence);
        return detailVO;
    }

    @Override
    public RecommendationDetailVO getRecommendationDetail(Long userId, String scenarioMode) {
        String normalizedMode = RecommendationScenarioMode.normalize(scenarioMode);
        RecommendationDetailVO detailVO = new RecommendationDetailVO();
        detailVO.setScenarioMode(normalizedMode);
        detailVO.setScenarioLabel(RecommendationScenarioMode.labelOf(normalizedMode));

        List<RecommendationResultEntity> resultEntities = recommendationQueryRepository.listByRequestUserId(userId);
        int size = resultEntities.size();
        detailVO.setRecallCandidatesCount(size);
        detailVO.setRecallCandidateCount(size);
        if (resultEntities.isEmpty()) {
            detailVO.setRankingDetails(List.of());
            detailVO.setRerankRuleHits(List.of());
            detailVO.setExplanationEvidence(List.of());
            return detailVO;
        }

        detailVO.setRequestTraceId(resultEntities.getFirst().getRequestTraceId());

        Map<Long, UserEntity> userCache = userService.listByIds(resultEntities.stream()
                        .map(RecommendationResultEntity::getTargetUserId)
                        .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, user -> user));

        List<Map<String, Object>> rankingDetails = new ArrayList<>();
        List<Map<String, Object>> rerankRuleHits = new ArrayList<>();
        List<Map<String, Object>> explanationEvidence = new ArrayList<>();
        for (RecommendationResultEntity entity : resultEntities) {
            ExplanationVO explanationVO = explanationService.getByRecommendationId(entity.getId());

            RecommendationItemVO itemVO = new RecommendationItemVO();
            itemVO.setRecommendationId(entity.getId() == null ? null : String.valueOf(entity.getId()));
            itemVO.setTargetUserId(entity.getTargetUserId());
            itemVO.setTargetNickname(resolveTargetNickname(entity.getTargetUserId(), userCache));
            itemVO.setRecallScore(entity.getRecallScore());
            itemVO.setRankScore(entity.getRankScore());
            itemVO.setInterestScore(extractDecimal(explanationVO, "interestScore", entity.getRankScore()));
            itemVO.setRerankScore(entity.getRerankScore());
            itemVO.setCampusScore(extractDecimal(explanationVO, "campusScore", entity.getRerankScore()));
            itemVO.setTrustScore(extractDecimal(explanationVO, "trustScore", BigDecimal.ZERO));
            itemVO.setFinalScore(entity.getFinalScore());
            itemVO.setRankNo(entity.getRankNo());
            itemVO.setScenarioMode(extractString(explanationVO, "scenarioMode", normalizedMode));
            itemVO.setScenarioLabel(extractString(
                    explanationVO,
                    "scenarioLabel",
                    RecommendationScenarioMode.labelOf(itemVO.getScenarioMode())
            ));
            itemVO.getMatchedTags().addAll(extractMatchedTags(explanationVO));
            itemVO.getMatchedRules().addAll(extractMatchedRules(explanationVO));
            itemVO.getTrustReasons().addAll(extractStringList(explanationVO, "trustReasons"));
            itemVO.setExploration(extractBoolean(explanationVO, "exploration", false));
            itemVO.setExplorationScore(extractDecimal(explanationVO, "explorationScore", BigDecimal.ZERO));
            itemVO.setExplorationReason(extractString(explanationVO, "explorationReason", ""));
            itemVO.setRecommendationLabel(extractString(
                    explanationVO,
                    "recommendationLabel",
                    RecommendationScenarioMode.recommendationLabelOf(
                            itemVO.getScenarioMode(),
                            !itemVO.getMatchedRules().isEmpty()
                    )
            ));
            itemVO.setExplanation(explanationVO.getReasonText());
            itemVO.setReasonText(explanationVO.getReasonText());
            detailVO.getItems().add(itemVO);

            rankingDetails.add(buildStoredRankingDetail(entity, explanationVO));
            rerankRuleHits.add(buildStoredRerankRuleHit(entity, explanationVO));
            explanationEvidence.add(buildStoredExplanationEvidence(entity, explanationVO));
        }

        detailVO.setRankingDetails(rankingDetails);
        detailVO.setRerankRuleHits(rerankRuleHits);
        detailVO.setExplanationEvidence(explanationEvidence);
        return detailVO;
    }

    private RecommendationItemVO toRecommendationItem(RankingCandidateModel candidate,
                                                      ExplanationVO explanationVO,
                                                      Long recommendationId,
                                                      String nickname,
                                                      int rankNo) {
        RecommendationItemVO itemVO = new RecommendationItemVO();
        itemVO.setRecommendationId(recommendationId == null ? null : String.valueOf(recommendationId));
        itemVO.setTargetUserId(candidate.getTargetUserId());
        itemVO.setTargetNickname(nickname);
        itemVO.setRecallScore(candidate.getRecallScore());
        itemVO.setRankScore(candidate.getRankScore());
        itemVO.setInterestScore(candidate.getInterestScore() == null ? candidate.getRankScore() : candidate.getInterestScore());
        itemVO.setRerankScore(candidate.getRerankScore());
        itemVO.setCampusScore(candidate.getCampusScore() == null ? candidate.getRerankScore() : candidate.getCampusScore());
        itemVO.setTrustScore(candidate.getTrustScore());
        itemVO.setFinalScore(candidate.getFinalScore());
        itemVO.setRankNo(rankNo);
        itemVO.setScenarioMode(candidate.getScenarioMode());
        itemVO.setScenarioLabel(candidate.getScenarioLabel());
        itemVO.getMatchedTags().addAll(extractMatchedTags(candidate));
        itemVO.getMatchedRules().addAll(extractMatchedRules(candidate));
        itemVO.getTrustReasons().addAll(candidate.getTrustReasons());
        itemVO.setExploration(candidate.isExploration());
        itemVO.setExplorationScore(candidate.getExplorationScore());
        itemVO.setExplorationReason(candidate.getExplorationReason());
        itemVO.setRecommendationLabel(buildRecommendationLabel(candidate));
        String explanation = explanationVO.getReasonText();
        itemVO.setExplanation(explanation);
        itemVO.setReasonText(explanation);
        return itemVO;
    }

    private Map<Long, Long> saveRecommendationResults(Long requestUserId,
                                                      String traceId,
                                                      List<RankingCandidateModel> topList) {
        Map<Long, Long> recommendationIdMap = new LinkedHashMap<>();
        int rankNo = 1;
        for (RankingCandidateModel candidate : topList) {
            RecommendationResultEntity entity = new RecommendationResultEntity();
            entity.setRequestUserId(requestUserId);
            entity.setTargetUserId(candidate.getTargetUserId());
            entity.setRecallScore(candidate.getRecallScore());
            entity.setRankScore(candidate.getRankScore());
            entity.setRerankScore(candidate.getRerankScore());
            entity.setFinalScore(candidate.getFinalScore());
            entity.setRankNo(rankNo++);
            entity.setRequestTraceId(traceId);
            entity.setCreatedAt(LocalDateTime.now());
            recommendationResultMapper.insert(entity);
            recommendationIdMap.put(candidate.getTargetUserId(), entity.getId());
        }
        return recommendationIdMap;
    }

    private String resolveTargetNickname(Long targetUserId, Map<Long, UserEntity> userCache) {
        UserEntity targetUser = userCache.computeIfAbsent(targetUserId, userService::getById);
        if (targetUser == null || targetUser.getNickname() == null || targetUser.getNickname().isBlank()) {
            return "用户-" + targetUserId;
        }
        return targetUser.getNickname();
    }

    private List<Map<String, Object>> buildRankingDetails(List<RankingCandidateModel> topList) {
        List<Map<String, Object>> rankingDetails = new ArrayList<>();
        for (RankingCandidateModel candidate : topList) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("targetUserId", candidate.getTargetUserId());
            item.put("scenarioMode", candidate.getScenarioMode());
            item.put("interestScore", candidate.getInterestScore());
            item.put("rankScore", candidate.getRankScore());
            item.put("exploration", candidate.isExploration());
            item.put("explorationScore", candidate.getExplorationScore());
            item.put("explorationReason", candidate.getExplorationReason());
            item.put("contributions", candidate.getContributions());
            rankingDetails.add(item);
        }
        return rankingDetails;
    }

    private List<Map<String, Object>> buildRerankRuleHits(List<RankingCandidateModel> topList) {
        List<Map<String, Object>> rerankRuleHits = new ArrayList<>();
        for (RankingCandidateModel candidate : topList) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("targetUserId", candidate.getTargetUserId());
            item.put("scenarioMode", candidate.getScenarioMode());
            item.put("campusScore", candidate.getCampusScore());
            item.put("trustScore", candidate.getTrustScore());
            item.put("trustReasons", candidate.getTrustReasons());
            item.put("exploration", candidate.isExploration());
            item.put("ruleHits", candidate.getRuleHits());
            rerankRuleHits.add(item);
        }
        return rerankRuleHits;
    }

    private Map<String, Object> buildExplanationEvidenceItem(Long recommendationId,
                                                             RankingCandidateModel candidate,
                                                             ExplanationVO explanationVO) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("recommendationId", recommendationId);
        item.put("targetUserId", candidate.getTargetUserId());
        item.put("scenarioMode", candidate.getScenarioMode());
        item.put("scenarioLabel", candidate.getScenarioLabel());
        item.put("exploration", candidate.isExploration());
        item.put("explorationScore", candidate.getExplorationScore());
        item.put("explorationReason", candidate.getExplorationReason());
        item.put("reasonText", explanationVO.getReasonText());
        item.put("evidence", explanationVO.getEvidence());
        item.put("contribution", explanationVO.getContribution());
        return item;
    }

    private Map<String, Object> buildStoredRankingDetail(RecommendationResultEntity entity, ExplanationVO explanationVO) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("recommendationId", entity.getId());
        item.put("targetUserId", entity.getTargetUserId());
        item.put("scenarioMode", extractString(explanationVO, "scenarioMode", null));
        item.put("interestScore", extractDecimal(explanationVO, "interestScore", entity.getRankScore()));
        item.put("rankScore", entity.getRankScore());
        item.put("finalScore", entity.getFinalScore());
        item.put("exploration", extractBoolean(explanationVO, "exploration", false));
        item.put("explorationScore", extractDecimal(explanationVO, "explorationScore", BigDecimal.ZERO));
        item.put("explorationReason", extractString(explanationVO, "explorationReason", ""));
        item.put("contributions", explanationVO.getContribution());
        return item;
    }

    private Map<String, Object> buildStoredRerankRuleHit(RecommendationResultEntity entity, ExplanationVO explanationVO) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("recommendationId", entity.getId());
        item.put("targetUserId", entity.getTargetUserId());
        item.put("campusScore", extractDecimal(explanationVO, "campusScore", entity.getRerankScore()));
        item.put("trustScore", extractDecimal(explanationVO, "trustScore", BigDecimal.ZERO));
        item.put("trustReasons", extractStringList(explanationVO, "trustReasons"));
        item.put("exploration", extractBoolean(explanationVO, "exploration", false));
        item.put("ruleHits", extractRuleHits(explanationVO));
        return item;
    }

    private Map<String, Object> buildStoredExplanationEvidence(RecommendationResultEntity entity, ExplanationVO explanationVO) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("recommendationId", entity.getId());
        item.put("targetUserId", entity.getTargetUserId());
        item.put("scenarioMode", extractString(explanationVO, "scenarioMode", null));
        item.put("scenarioLabel", extractString(explanationVO, "scenarioLabel", null));
        item.put("exploration", extractBoolean(explanationVO, "exploration", false));
        item.put("explorationScore", extractDecimal(explanationVO, "explorationScore", BigDecimal.ZERO));
        item.put("explorationReason", extractString(explanationVO, "explorationReason", ""));
        item.put("reasonText", explanationVO.getReasonText());
        item.put("evidence", explanationVO.getEvidence());
        item.put("contribution", explanationVO.getContribution());
        return item;
    }

    private Object extractRuleHits(ExplanationVO explanationVO) {
        Object evidence = explanationVO.getEvidence();
        if (!(evidence instanceof Map<?, ?> evidenceMap)) {
            return List.of();
        }
        Object ruleHits = evidenceMap.get("ruleHits");
        return ruleHits == null ? List.of() : ruleHits;
    }

    private List<String> extractMatchedTags(RankingCandidateModel candidate) {
        return candidate.getContributions().stream()
                .map(contribution -> contribution.getTagName())
                .filter(tagName -> tagName != null && !tagName.isBlank())
                .limit(3)
                .toList();
    }

    private List<String> extractMatchedRules(RankingCandidateModel candidate) {
        return candidate.getRuleHits().stream()
                .filter(ruleHit -> Boolean.TRUE.equals(ruleHit.getHit()))
                .map(ruleHit -> ruleHit.getRuleDesc())
                .filter(ruleDesc -> ruleDesc != null && !ruleDesc.isBlank())
                .toList();
    }

    private List<String> extractMatchedTags(ExplanationVO explanationVO) {
        if (!(explanationVO.getContribution() instanceof List<?> contributions)) {
            return List.of();
        }
        List<String> matchedTags = new ArrayList<>();
        for (Object contribution : contributions) {
            if (contribution instanceof Map<?, ?> contributionMap) {
                Object tagName = contributionMap.get("tagName");
                if (tagName instanceof String tag && !tag.isBlank()) {
                    matchedTags.add(tag);
                }
            }
        }
        return matchedTags.stream().limit(3).toList();
    }

    private List<String> extractMatchedRules(ExplanationVO explanationVO) {
        Object ruleHits = extractRuleHits(explanationVO);
        if (!(ruleHits instanceof List<?> hits)) {
            return List.of();
        }
        List<String> matchedRules = new ArrayList<>();
        for (Object hit : hits) {
            if (hit instanceof Map<?, ?> hitMap) {
                Object ruleDesc = hitMap.get("ruleDesc");
                Object hitValue = hitMap.get("hit");
                if ((hitValue == null || Boolean.parseBoolean(String.valueOf(hitValue)))
                        && ruleDesc instanceof String desc
                        && !desc.isBlank()) {
                    matchedRules.add(desc);
                }
            }
        }
        return matchedRules;
    }

    private List<String> extractStringList(ExplanationVO explanationVO, String key) {
        Object evidence = explanationVO.getEvidence();
        if (!(evidence instanceof Map<?, ?> evidenceMap)) {
            return List.of();
        }
        Object value = evidenceMap.get(key);
        if (!(value instanceof List<?> items)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof String text && !text.isBlank()) {
                result.add(text);
            }
        }
        return result;
    }

    private BigDecimal extractDecimal(ExplanationVO explanationVO, String key, BigDecimal fallback) {
        Object evidence = explanationVO.getEvidence();
        if (!(evidence instanceof Map<?, ?> evidenceMap)) {
            return fallback;
        }
        Object value = evidenceMap.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private boolean extractBoolean(ExplanationVO explanationVO, String key, boolean fallback) {
        Object evidence = explanationVO.getEvidence();
        if (!(evidence instanceof Map<?, ?> evidenceMap)) {
            return fallback;
        }
        Object value = evidenceMap.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text && !text.isBlank()) {
            return Boolean.parseBoolean(text);
        }
        return fallback;
    }

    private String extractString(ExplanationVO explanationVO, String key, String fallback) {
        Object evidence = explanationVO.getEvidence();
        if (!(evidence instanceof Map<?, ?> evidenceMap)) {
            return fallback;
        }
        Object value = evidenceMap.get(key);
        return value instanceof String text && !text.isBlank() ? text : fallback;
    }

    private String buildRecommendationLabel(RankingCandidateModel candidate) {
        return RecommendationScenarioMode.recommendationLabelOf(
                candidate.getScenarioMode(),
                !extractMatchedRules(candidate).isEmpty()
        );
    }
}
