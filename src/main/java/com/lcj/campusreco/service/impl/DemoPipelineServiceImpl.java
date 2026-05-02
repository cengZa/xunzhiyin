package com.lcj.campusreco.service.impl;

import com.lcj.campusreco.common.constant.RecommendationScenarioMode;
import com.lcj.campusreco.common.constant.FeedbackType;
import com.lcj.campusreco.config.RecommendationTuningContext;
import com.lcj.campusreco.domain.entity.RecommendationResultEntity;
import com.lcj.campusreco.domain.entity.UserFeedbackEntity;
import com.lcj.campusreco.domain.entity.UserTagRelationEntity;
import com.lcj.campusreco.domain.entity.TagEntity;
import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.model.ContributionItemModel;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.RuleHitModel;
import com.lcj.campusreco.domain.model.TagWeightModel;
import com.lcj.campusreco.domain.model.UserProfileModel;
import com.lcj.campusreco.domain.vo.DemoPipelineVO;
import com.lcj.campusreco.domain.vo.ExplanationVO;
import com.lcj.campusreco.infra.redis.RecallIndexRepository;
import com.lcj.campusreco.mapper.RecommendationResultMapper;
import com.lcj.campusreco.mapper.UserFeedbackMapper;
import com.lcj.campusreco.mapper.UserTagRelationMapper;
import com.lcj.campusreco.service.DemoPipelineService;
import com.lcj.campusreco.service.ExplorationService;
import com.lcj.campusreco.service.ExplanationService;
import com.lcj.campusreco.service.ProfileService;
import com.lcj.campusreco.service.RankingService;
import com.lcj.campusreco.service.RecallService;
import com.lcj.campusreco.service.RerankService;
import com.lcj.campusreco.service.TagService;
import com.lcj.campusreco.service.UserService;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DemoPipelineServiceImpl implements DemoPipelineService {

    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
    private static final BigDecimal PROFILE_WEIGHT = new BigDecimal("0.4");
    private static final BigDecimal TAG_WEIGHT = new BigDecimal("0.3");
    private static final BigDecimal FOLLOW_WEIGHT = new BigDecimal("0.3");
    private static final BigDecimal PROFILE_STEP = new BigDecimal("0.1");
    private static final BigDecimal TAG_MAX_COUNT = new BigDecimal("4");
    private static final BigDecimal FOLLOW_MAX_COUNT = new BigDecimal("3");

    private final ProfileService profileService;
    private final RecallService recallService;
    private final RankingService rankingService;
    private final RerankService rerankService;
    private final ExplorationService explorationService;
    private final ExplanationService explanationService;
    private final UserService userService;
    private final TagService tagService;
    private final RecommendationTuningContext tuningContext;
    private final RecommendationResultMapper recommendationResultMapper;
    private final UserTagRelationMapper userTagRelationMapper;
    private final UserFeedbackMapper userFeedbackMapper;
    private final RecallIndexRepository recallIndexRepository;

    public DemoPipelineServiceImpl(ProfileService profileService,
                                   RecallService recallService,
                                   RankingService rankingService,
                                   RerankService rerankService,
                                   ExplorationService explorationService,
                                   ExplanationService explanationService,
                                   UserService userService,
                                   TagService tagService,
                                   RecommendationTuningContext tuningContext,
                                   RecommendationResultMapper recommendationResultMapper,
                                   UserTagRelationMapper userTagRelationMapper,
                                   UserFeedbackMapper userFeedbackMapper,
                                   RecallIndexRepository recallIndexRepository) {
        this.profileService = profileService;
        this.recallService = recallService;
        this.rankingService = rankingService;
        this.rerankService = rerankService;
        this.explorationService = explorationService;
        this.explanationService = explanationService;
        this.userService = userService;
        this.tagService = tagService;
        this.tuningContext = tuningContext;
        this.recommendationResultMapper = recommendationResultMapper;
        this.userTagRelationMapper = userTagRelationMapper;
        this.userFeedbackMapper = userFeedbackMapper;
        this.recallIndexRepository = recallIndexRepository;
    }

    @Override
    public DemoPipelineVO buildPipeline(Long userId, Integer topK, String scenarioMode) {
        int effectiveTopK = topK == null || topK < 1 ? 3 : topK;
        String normalizedMode = RecommendationScenarioMode.normalize(scenarioMode);

        UserEntity requestUser = userService.getById(userId);
        List<UserTagRelationEntity> requestRelations = userTagRelationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserTagRelationEntity>()
                        .eq(UserTagRelationEntity::getUserId, userId)
                        .orderByDesc(UserTagRelationEntity::getSelectedAt)
                        .orderByDesc(UserTagRelationEntity::getUpdatedAt)
        );
        Map<Long, TagEntity> requestTagMap = tagService.listUserTags(userId).stream()
                .collect(Collectors.toMap(TagEntity::getId, tag -> tag, (left, right) -> left, LinkedHashMap::new));
        UserProfileModel profileModel = profileService.getProfile(userId);
        if (profileModel.getVector().isEmpty()) {
            profileModel = profileService.buildProfile(userId, "demo_pipeline");
        }

        Set<Long> candidateUserIds = recallService.recallCandidateUserIds(profileModel);
        List<RankingCandidateModel> rankingList = rankingService.rank(userId, candidateUserIds);
        List<RankingCandidateModel> rerankedList;
        List<RankingCandidateModel> finalList;
        try (RecommendationTuningContext.Scope ignored =
                     tuningContext.withOverrides(null, null, normalizedMode, true)) {
            rerankedList = rerankService.rerank(userId, cloneCandidates(rankingList));
            finalList = explorationService.apply(userId, rerankedList, effectiveTopK, normalizedMode);
        }

        Map<Long, UserEntity> userCache = userService.listByIds(new ArrayList<>(candidateUserIds))
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, user -> user));
        Map<Long, UserProfileModel> candidateProfiles = new HashMap<>();
        for (Long candidateUserId : candidateUserIds) {
            candidateProfiles.put(candidateUserId, profileService.getProfile(candidateUserId));
        }
        Map<Long, String> recallSourceMap = buildRecallSourceMap(profileModel);

        DemoPipelineVO pipeline = new DemoPipelineVO();
        pipeline.setUserId(userId);
        pipeline.setTopK(effectiveTopK);
        pipeline.setRecallCandidateCount(candidateUserIds.size());
        pipeline.setScenarioMode(normalizedMode);
        pipeline.setScenarioLabel(RecommendationScenarioMode.labelOf(normalizedMode));
        pipeline.setRequestUser(buildUserSummary(requestUser));
        pipeline.setScenarioStage(buildScenarioStage(normalizedMode));
        pipeline.setInputTags(buildInputTags(requestRelations, requestTagMap));
        pipeline.setProfileStage(buildProfileStage(profileModel, requestRelations, requestTagMap));
        pipeline.setRecallStage(buildRecallStage(rankingList, userCache, candidateProfiles, profileModel, recallSourceMap));
        pipeline.setRankingStage(buildRankingStage(rankingList, userCache, candidateProfiles, profileModel));
        pipeline.setRerankStage(buildRerankStage(rerankedList, userCache));
        Map<Long, Long> recommendationIdMap = saveRecommendationResults(userId, UUID.randomUUID().toString(), finalList);
        explanationService.batchSaveExplanation(finalList, recommendationIdMap);
        pipeline.setFinalStage(buildFinalStage(finalList, userCache, recommendationIdMap));
        return pipeline;
    }

    private Map<String, Object> buildScenarioStage(String scenarioMode) {
        String normalizedMode = RecommendationScenarioMode.normalize(scenarioMode);
        Map<String, Object> stage = new LinkedHashMap<>();
        stage.put("scenarioMode", normalizedMode);
        stage.put("scenarioLabel", RecommendationScenarioMode.labelOf(normalizedMode));
        switch (normalizedMode) {
            case RecommendationScenarioMode.STUDY_PARTNER -> {
                stage.put("objective", "优先寻找专业方向接近、年级差距小、学术标签重合度高的学习搭子。");
                stage.put("modeChanges", List.of(
                        "专业相关规则权重提高到 1.4 倍",
                        "年级接近规则权重提高到 1.2 倍",
                        "社团重合规则降到 0.4 倍"
                ));
            }
            case RecommendationScenarioMode.CLUB_PARTNER -> {
                stage.put("objective", "优先寻找社团、志愿和校园活动重合度更高的活动搭子。");
                stage.put("modeChanges", List.of(
                        "社团重合规则权重提高到 1.8 倍",
                        "年级接近规则保持 1.0 倍",
                        "专业相关规则降到 0.5 倍"
                ));
            }
            default -> {
                stage.put("objective", "优先寻找兴趣同频的用户，同时保留少量高潜探索位。");
                stage.put("modeChanges", List.of(
                        "社团重合规则保持 1.0 倍",
                        "专业相关规则降到 0.7 倍",
                        "年级接近规则降到 0.6 倍",
                        "允许保留 1 个轻量探索位"
                ));
            }
        }
        stage.put("finalScoreFormula", "finalScore = interestScore + campusScore + trustScore × trustWeight");
        return stage;
    }

    private Map<String, Object> buildUserSummary(UserEntity user) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (user == null) {
            return summary;
        }
        summary.put("userId", String.valueOf(user.getId()));
        summary.put("nickname", user.getNickname());
        summary.put("major", user.getMajor());
        summary.put("college", user.getCollege());
        summary.put("grade", user.getGrade());
        summary.put("bio", user.getBio());
        summary.put("generatedAt", LocalDateTime.now().toString());
        return summary;
    }

    private List<Map<String, Object>> buildInputTags(List<UserTagRelationEntity> relations, Map<Long, TagEntity> tagMap) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserTagRelationEntity relation : relations) {
            TagEntity tag = tagMap.get(relation.getTagId());
            if (tag == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tagId", String.valueOf(tag.getId()));
            item.put("tagName", tag.getTagName());
            item.put("tagType", tag.getTagType());
            item.put("tagTypeLabel", tagTypeLabel(tag.getTagType()));
            item.put("tagDesc", tag.getTagDesc());
            item.put("sourceType", relation.getSourceType());
            item.put("selectedAt", relation.getSelectedAt() == null ? null : relation.getSelectedAt().toString());
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> buildProfileStage(UserProfileModel profileModel,
                                                  List<UserTagRelationEntity> relations,
                                                  Map<Long, TagEntity> tagMap) {
        Map<String, Object> stage = new LinkedHashMap<>();
        stage.put("profileVersion", profileModel.getProfileVersion());
        stage.put("vectorSize", profileModel.getVector().size());
        stage.put("tagWeightCount", profileModel.getTagWeights().size());
        stage.put("weightFormulaLabel", "改进 TF-IDF");
        stage.put("weightFormula", "finalWeight = tf × idf × timeDecay × weightSeed；当前 idf 固定为 1.0。");
        stage.put("profileTopTagLimit", tuningContext.getProfileTopTagLimit());
        stage.put("tagWeights", profileModel.getTagWeights().stream()
                .sorted(Comparator.comparing(TagWeightModel::getFinalWeight, Comparator.nullsLast(BigDecimal::compareTo)).reversed())
                .map(tagWeight -> toTagWeightItem(tagWeight, relations, tagMap))
                .toList());
        stage.put("topKTags", profileModel.getTopKTags().stream()
                .map(tagWeight -> toTagWeightItem(tagWeight, relations, tagMap))
                .toList());
        stage.put("vectorPreview", profileModel.getVector().entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(8)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("tagId", String.valueOf(entry.getKey()));
                    item.put("tagName", safeTagName(tagMap.get(entry.getKey()), entry.getKey()));
                    item.put("weight", entry.getValue());
                    return item;
                })
                .toList());
        return stage;
    }

    private Map<String, Object> toTagWeightItem(TagWeightModel tagWeightModel,
                                                List<UserTagRelationEntity> relations,
                                                Map<Long, TagEntity> tagMap) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("tagId", String.valueOf(tagWeightModel.getTagId()));
        item.put("tagName", tagWeightModel.getTagName());
        item.put("tagType", tagWeightModel.getTagType());
        item.put("tagTypeLabel", tagTypeLabel(tagWeightModel.getTagType()));
        item.put("tf", tagWeightModel.getTf());
        item.put("idf", tagWeightModel.getIdf());
        item.put("timeDecay", tagWeightModel.getTimeDecay());
        item.put("finalWeight", tagWeightModel.getFinalWeight());
        item.put("formulaText",
                String.format("tf(%s) × idf(%s) × timeDecay(%s) × weightSeed(1.0000)",
                        scoreText(tagWeightModel.getTf()),
                        scoreText(tagWeightModel.getIdf()),
                        scoreText(tagWeightModel.getTimeDecay())));
        item.put("relations", relations.stream()
                .filter(relation -> tagWeightModel.getTagId().equals(relation.getTagId()))
                .map(relation -> {
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("sourceType", relation.getSourceType());
                    detail.put("selectedAt", relation.getSelectedAt() == null ? null : relation.getSelectedAt().toString());
                    detail.put("weightSeed", relation.getWeightSeed());
                    detail.put("tagDomain", tagTypeLabel(resolveTagType(tagMap.get(relation.getTagId()))));
                    return detail;
                })
                .toList());
        return item;
    }

    private List<Map<String, Object>> buildRecallStage(List<RankingCandidateModel> rankingList,
                                                       Map<Long, UserEntity> userCache,
                                                       Map<Long, UserProfileModel> candidateProfiles,
                                                       UserProfileModel requestProfile,
                                                       Map<Long, String> recallSourceMap) {
        return rankingList.stream()
                .sorted(Comparator.comparing(RankingCandidateModel::getRecallScore, Comparator.nullsLast(BigDecimal::compareTo)).reversed()
                        .thenComparing(Comparator.comparing(RankingCandidateModel::getRankScore, Comparator.nullsLast(BigDecimal::compareTo)).reversed()))
                .map(candidate -> buildRecallStageItem(
                        candidate,
                        userCache,
                        candidateProfiles.get(candidate.getTargetUserId()),
                        requestProfile,
                        recallSourceMap
                ))
                .toList();
    }

    private List<Map<String, Object>> buildRankingStage(List<RankingCandidateModel> rankingList,
                                                        Map<Long, UserEntity> userCache,
                                                        Map<Long, UserProfileModel> candidateProfiles,
                                                        UserProfileModel requestProfile) {
        return rankingList.stream()
                .sorted(Comparator.comparing(RankingCandidateModel::getRankScore, Comparator.nullsLast(BigDecimal::compareTo)).reversed())
                .map(candidate -> buildRankingStageItem(candidate, userCache, candidateProfiles.get(candidate.getTargetUserId()), requestProfile))
                .toList();
    }

    private List<Map<String, Object>> buildFinalStage(List<RankingCandidateModel> finalList,
                                                      Map<Long, UserEntity> userCache,
                                                      Map<Long, Long> recommendationIdMap) {
        int[] rankNo = {1};
        return finalList.stream()
                .map(candidate -> {
                    Map<String, Object> item = buildCandidateStageItem(candidate, userCache, true, true);
                    item.put("rankNo", rankNo[0]++);
                    Long recommendationId = recommendationIdMap.get(candidate.getTargetUserId());
                    item.put("recommendationId", recommendationId == null ? null : String.valueOf(recommendationId));
                    ExplanationVO explanationVO = explanationService.generate(candidate);
                    item.put("reasonText", explanationVO.getReasonText());
                    item.put("evidence", explanationVO.getEvidence());
                    item.put("contribution", explanationVO.getContribution());
                    return item;
                })
                .toList();
    }

    private Map<Long, Long> saveRecommendationResults(Long requestUserId,
                                                      String traceId,
                                                      List<RankingCandidateModel> finalList) {
        Map<Long, Long> recommendationIdMap = new LinkedHashMap<>();
        int rankNo = 1;
        for (RankingCandidateModel candidate : finalList) {
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

    private Map<String, Object> buildCandidateStageItem(RankingCandidateModel candidate,
                                                        Map<Long, UserEntity> userCache,
                                                        boolean includeContribution,
                                                        boolean includeRerank) {
        Map<String, Object> item = new LinkedHashMap<>();
        UserEntity user = userCache.get(candidate.getTargetUserId());
        item.put("targetUserId", String.valueOf(candidate.getTargetUserId()));
        item.put("targetNickname", resolveNickname(user, candidate.getTargetUserId()));
        item.put("major", user == null ? null : user.getMajor());
        item.put("college", user == null ? null : user.getCollege());
        item.put("grade", user == null ? null : user.getGrade());
        item.put("recallScore", candidate.getRecallScore());
        item.put("rankScore", candidate.getRankScore());
        item.put("interestScore", candidate.getInterestScore() == null ? candidate.getRankScore() : candidate.getInterestScore());
        if (includeContribution) {
            item.put("matchedTags", candidate.getContributions().stream()
                    .map(ContributionItemModel::getTagName)
                    .filter(tagName -> tagName != null && !tagName.isBlank())
                    .limit(4)
                    .toList());
            item.put("contributions", candidate.getContributions());
        }
        if (includeRerank) {
            item.put("campusScore", candidate.getCampusScore());
            item.put("trustScore", candidate.getTrustScore());
            item.put("trustReasons", candidate.getTrustReasons());
            item.put("ruleHits", candidate.getRuleHits().stream()
                    .filter(ruleHit -> Boolean.TRUE.equals(ruleHit.getHit()))
                    .map(this::toRuleHitItem)
                    .toList());
            item.put("exploration", candidate.isExploration());
            item.put("explorationScore", candidate.getExplorationScore());
            item.put("explorationReason", candidate.getExplorationReason());
            item.put("finalScore", candidate.getFinalScore());
        }
        return item;
    }

    private Map<String, Object> buildRecallStageItem(RankingCandidateModel candidate,
                                                     Map<Long, UserEntity> userCache,
                                                     UserProfileModel candidateProfile,
                                                     UserProfileModel requestProfile,
                                                     Map<Long, String> recallSourceMap) {
        Map<String, Object> item = buildCandidateStageItem(candidate, userCache, false, false);
        List<Map<String, Object>> recallTags = requestProfile.getTopKTags().stream()
                .map(tagWeight -> {
                    boolean matched = candidateProfile != null && candidateProfile.getVector().containsKey(tagWeight.getTagId());
                    Map<String, Object> trace = new LinkedHashMap<>();
                    trace.put("tagId", String.valueOf(tagWeight.getTagId()));
                    trace.put("tagName", tagWeight.getTagName());
                    trace.put("tagType", tagWeight.getTagType());
                    trace.put("tagTypeLabel", tagTypeLabel(tagWeight.getTagType()));
                    trace.put("requestWeight", tagWeight.getFinalWeight());
                    trace.put("matched", matched);
                    trace.put("recallSource", recallSourceMap.getOrDefault(tagWeight.getTagId(), "db_relation"));
                    return trace;
                })
                .filter(trace -> Boolean.TRUE.equals(trace.get("matched")))
                .toList();
        item.put("recallFormulaLabel", "重叠召回标签数");
        item.put("recallFormula", "recallScore = 命中的 Top-K 画像标签数。当前召回阶段不做额外加权，只看候选人是否命中了请求用户的 Top-K 标签。");
        item.put("matchedRecallTags", recallTags.stream().map(trace -> trace.get("tagName")).toList());
        item.put("recallTrace", recallTags);
        return item;
    }

    private Map<String, Object> buildRankingStageItem(RankingCandidateModel candidate,
                                                      Map<Long, UserEntity> userCache,
                                                      UserProfileModel candidateProfile,
                                                      UserProfileModel requestProfile) {
        Map<String, Object> item = buildCandidateStageItem(candidate, userCache, true, false);
        Map<String, Object> cosineDetail = buildCosineDetail(requestProfile, candidateProfile);
        item.put("interestScore", candidate.getRankScore());
        item.put("rankingFormulaLabel", "余弦相似度");
        item.put("rankingFormula", "rankScore = dot(requestVector, candidateVector) / (|requestVector| × |candidateVector|)");
        item.put("rankingDetail", cosineDetail);
        return item;
    }

    private Map<String, Object> toRuleHitItem(RuleHitModel ruleHitModel) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("ruleCode", ruleHitModel.getRuleCode());
        item.put("ruleDesc", ruleHitModel.getRuleDesc());
        item.put("adjustScore", ruleHitModel.getAdjustScore());
        return item;
    }

    private List<Map<String, Object>> buildRerankStage(List<RankingCandidateModel> rerankedList,
                                                       Map<Long, UserEntity> userCache) {
        return rerankedList.stream()
                .map(candidate -> {
                    Map<String, Object> item = buildCandidateStageItem(candidate, userCache, true, true);
                    item.put("finalScoreFormulaLabel", "兴趣分 + 场景分 + 可信分 × trustWeight");
                    item.put("trustWeight", tuningContext.isTrustEnabled() ? new BigDecimal("0.15") : BigDecimal.ZERO);
                    item.put("ruleDetails", candidate.getRuleHits().stream()
                            .map(this::toRuleDetailItem)
                            .toList());
                    item.put("trustBreakdown", buildTrustBreakdown(userCache.get(candidate.getTargetUserId()), candidate));
                    return item;
                })
                .toList();
    }

    private Map<String, Object> toRuleDetailItem(RuleHitModel ruleHitModel) {
        BigDecimal baseScore = resolveRuleBaseScore(ruleHitModel.getRuleCode());
        BigDecimal multiplier = resolveScenarioMultiplier(
                tuningContext.getScenarioMode(),
                ruleHitModel.getRuleCode()
        );
        BigDecimal weightedScore = defaultScore(ruleHitModel.getAdjustScore()).multiply(tuningContext.getRerankWeightScale())
                .setScale(4, RoundingMode.HALF_UP);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("ruleCode", ruleHitModel.getRuleCode());
        item.put("ruleDesc", ruleHitModel.getRuleDesc());
        item.put("hit", ruleHitModel.getHit());
        item.put("baseScore", baseScore);
        item.put("scenarioMultiplier", multiplier);
        item.put("scenarioAdjustedScore", ruleHitModel.getAdjustScore());
        item.put("rerankWeightScale", tuningContext.getRerankWeightScale());
        item.put("weightedContribution", weightedScore);
        item.put("formulaText", "weightedContribution = baseScore × scenarioMultiplier × rerankWeightScale");
        return item;
    }

    private Map<String, Object> buildTrustBreakdown(UserEntity candidateUser, RankingCandidateModel candidate) {
        Map<String, Object> item = new LinkedHashMap<>();
        if (candidateUser == null) {
            return item;
        }
        BigDecimal profileScore = calculateProfileCompleteness(candidateUser);
        long tagCount = userTagRelationMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserTagRelationEntity>()
                        .eq(UserTagRelationEntity::getUserId, candidateUser.getId())
        );
        BigDecimal tagScore = BigDecimal.valueOf(Math.min(tagCount, TAG_MAX_COUNT.longValue()))
                .divide(TAG_MAX_COUNT, 4, RoundingMode.HALF_UP)
                .multiply(TAG_WEIGHT);
        long followCount = userFeedbackMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserFeedbackEntity>()
                        .eq(UserFeedbackEntity::getTargetUserId, candidateUser.getId())
                        .eq(UserFeedbackEntity::getFeedbackType, FeedbackType.FOLLOW)
        );
        BigDecimal followScore = BigDecimal.valueOf(Math.min(followCount, FOLLOW_MAX_COUNT.longValue()))
                .divide(FOLLOW_MAX_COUNT, 4, RoundingMode.HALF_UP)
                .multiply(FOLLOW_WEIGHT);
        item.put("profileScore", profileScore);
        item.put("profileRule", "昵称、专业、学院、个人简介长度>=10，各记 0.1，上限 0.4");
        item.put("tagScore", tagScore);
        item.put("tagRule", "tagScore = min(tagCount, 4) / 4 × 0.3");
        item.put("tagCount", tagCount);
        item.put("followScore", followScore);
        item.put("followRule", "historyFollowScore = min(followCount, 3) / 3 × 0.3");
        item.put("followCount", followCount);
        item.put("trustReasons", candidate.getTrustReasons());
        item.put("reasonThresholds", List.of(
                "资料完整：profileScore >= 0.3",
                "标签丰富：tagCount >= 3",
                "历史关注较多：followCount >= 2"
        ));
        return item;
    }

    private Map<String, Object> buildCosineDetail(UserProfileModel requestProfile, UserProfileModel candidateProfile) {
        Map<String, Object> detail = new LinkedHashMap<>();
        if (requestProfile == null || candidateProfile == null) {
            return detail;
        }
        BigDecimal dot = BigDecimal.ZERO;
        BigDecimal requestNormSquare = BigDecimal.ZERO;
        BigDecimal candidateNormSquare = BigDecimal.ZERO;
        int overlap = 0;
        for (Map.Entry<Long, BigDecimal> entry : requestProfile.getVector().entrySet()) {
            BigDecimal left = defaultScore(entry.getValue());
            BigDecimal right = defaultScore(candidateProfile.getVector().get(entry.getKey()));
            if (right.compareTo(BigDecimal.ZERO) > 0) {
                overlap++;
            }
            dot = dot.add(left.multiply(right, MATH_CONTEXT), MATH_CONTEXT);
            requestNormSquare = requestNormSquare.add(left.multiply(left, MATH_CONTEXT), MATH_CONTEXT);
        }
        for (BigDecimal value : candidateProfile.getVector().values()) {
            BigDecimal safe = defaultScore(value);
            candidateNormSquare = candidateNormSquare.add(safe.multiply(safe, MATH_CONTEXT), MATH_CONTEXT);
        }
        detail.put("overlapCount", overlap);
        detail.put("dotProduct", dot.setScale(4, RoundingMode.HALF_UP));
        detail.put("requestNorm", sqrt(requestNormSquare));
        detail.put("candidateNorm", sqrt(candidateNormSquare));
        detail.put("explanation", "排序阶段按余弦相似度比较两个画像向量；contribution 列表展示了重叠标签对 dotProduct 的贡献。");
        return detail;
    }

    private Map<Long, String> buildRecallSourceMap(UserProfileModel profileModel) {
        Map<Long, String> sourceMap = new HashMap<>();
        for (TagWeightModel tagWeight : profileModel.getTopKTags()) {
            sourceMap.put(tagWeight.getTagId(),
                    recallIndexRepository.getCandidateUserIdsByTag(tagWeight.getTagId()).isEmpty() ? "db_relation" : "redis_index");
        }
        return sourceMap;
    }

    private BigDecimal resolveRuleBaseScore(String ruleCode) {
        return switch (ruleCode) {
            case "MAJOR_RELATED" -> new BigDecimal("0.0800");
            case "GRADE_DIFF" -> new BigDecimal("0.0500");
            case "CLUB_OVERLAP" -> new BigDecimal("0.0400");
            default -> BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        };
    }

    private BigDecimal resolveScenarioMultiplier(String scenarioMode, String ruleCode) {
        String normalizedMode = RecommendationScenarioMode.normalize(scenarioMode);
        return switch (normalizedMode) {
            case RecommendationScenarioMode.STUDY_PARTNER -> switch (ruleCode) {
                case "MAJOR_RELATED" -> new BigDecimal("1.4");
                case "GRADE_DIFF" -> new BigDecimal("1.2");
                case "CLUB_OVERLAP" -> new BigDecimal("0.4");
                default -> BigDecimal.ONE;
            };
            case RecommendationScenarioMode.CLUB_PARTNER -> switch (ruleCode) {
                case "CLUB_OVERLAP" -> new BigDecimal("1.8");
                case "GRADE_DIFF" -> BigDecimal.ONE;
                case "MAJOR_RELATED" -> new BigDecimal("0.5");
                default -> BigDecimal.ONE;
            };
            default -> switch (ruleCode) {
                case "CLUB_OVERLAP" -> BigDecimal.ONE;
                case "MAJOR_RELATED" -> new BigDecimal("0.7");
                case "GRADE_DIFF" -> new BigDecimal("0.6");
                default -> BigDecimal.ONE;
            };
        };
    }

    private BigDecimal calculateProfileCompleteness(UserEntity candidateUser) {
        BigDecimal score = BigDecimal.ZERO;
        if (hasText(candidateUser.getNickname())) {
            score = score.add(PROFILE_STEP);
        }
        if (hasText(candidateUser.getMajor())) {
            score = score.add(PROFILE_STEP);
        }
        if (hasText(candidateUser.getCollege())) {
            score = score.add(PROFILE_STEP);
        }
        if (candidateUser.getBio() != null && candidateUser.getBio().trim().length() >= 10) {
            score = score.add(PROFILE_STEP);
        }
        return score.min(PROFILE_WEIGHT).setScale(4, RoundingMode.HALF_UP);
    }

    private String tagTypeLabel(String tagType) {
        return switch (tagType == null ? "" : tagType.toLowerCase()) {
            case "academic" -> "学术";
            case "hobby" -> "爱好";
            case "club" -> "社团";
            case "interest" -> "兴趣";
            default -> "未分类";
        };
    }

    private String resolveTagType(TagEntity tagEntity) {
        return tagEntity == null ? null : tagEntity.getTagType();
    }

    private String safeTagName(TagEntity tagEntity, Long tagId) {
        if (tagEntity == null || tagEntity.getTagName() == null || tagEntity.getTagName().isBlank()) {
            return "tag-" + tagId;
        }
        return tagEntity.getTagName();
    }

    private BigDecimal sqrt(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(Math.sqrt(value.doubleValue())).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultScore(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : value.setScale(4, RoundingMode.HALF_UP);
    }

    private String scoreText(BigDecimal value) {
        return defaultScore(value).toPlainString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String resolveNickname(UserEntity user, Long userId) {
        if (user == null || user.getNickname() == null || user.getNickname().isBlank()) {
            return "用户 " + userId;
        }
        return user.getNickname();
    }

    private List<RankingCandidateModel> cloneCandidates(List<RankingCandidateModel> rankingList) {
        List<RankingCandidateModel> copies = new ArrayList<>();
        for (RankingCandidateModel candidate : rankingList) {
            RankingCandidateModel copy = new RankingCandidateModel();
            copy.setTargetUserId(candidate.getTargetUserId());
            copy.setRecallScore(candidate.getRecallScore());
            copy.setRankScore(candidate.getRankScore());
            copy.setInterestScore(candidate.getInterestScore());
            copy.setRerankScore(candidate.getRerankScore());
            copy.setCampusScore(candidate.getCampusScore());
            copy.setTrustScore(candidate.getTrustScore());
            copy.setExplorationScore(candidate.getExplorationScore());
            copy.setFinalScore(candidate.getFinalScore());
            copy.setScenarioMode(candidate.getScenarioMode());
            copy.setScenarioLabel(candidate.getScenarioLabel());
            copy.setExploration(candidate.isExploration());
            copy.setExplorationReason(candidate.getExplorationReason());
            copy.setTrustReasons(new ArrayList<>(candidate.getTrustReasons()));
            copy.setContributions(new ArrayList<>(candidate.getContributions()));
            copy.setRuleHits(new ArrayList<>(candidate.getRuleHits()));
            copies.add(copy);
        }
        return copies;
    }
}
