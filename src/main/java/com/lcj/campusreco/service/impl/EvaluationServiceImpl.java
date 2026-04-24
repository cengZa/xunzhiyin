package com.lcj.campusreco.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcj.campusreco.common.constant.RecommendationScenarioMode;
import com.lcj.campusreco.config.RecommendationTuningContext;
import com.lcj.campusreco.domain.entity.TagEntity;
import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.entity.UserTagRelationEntity;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.UserProfileModel;
import com.lcj.campusreco.domain.vo.EvaluationBaselineVO;
import com.lcj.campusreco.domain.vo.EvaluationSummaryVO;
import com.lcj.campusreco.mapper.TagMapper;
import com.lcj.campusreco.mapper.UserMapper;
import com.lcj.campusreco.mapper.UserTagRelationMapper;
import com.lcj.campusreco.service.EvaluationService;
import com.lcj.campusreco.service.ExplanationService;
import com.lcj.campusreco.service.ProfileService;
import com.lcj.campusreco.service.RankingService;
import com.lcj.campusreco.service.RecallService;
import com.lcj.campusreco.service.RerankService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class EvaluationServiceImpl implements EvaluationService {

    private static final String PROXY_RULE = "shared_tags>=2 OR (same_major AND shared_tags>=1)";

    private final UserMapper userMapper;
    private final TagMapper tagMapper;
    private final UserTagRelationMapper userTagRelationMapper;
    private final ProfileService profileService;
    private final RecallService recallService;
    private final RankingService rankingService;
    private final RerankService rerankService;
    private final ExplanationService explanationService;
    private final RecommendationTuningContext tuningContext;

    public EvaluationServiceImpl(UserMapper userMapper,
                                 TagMapper tagMapper,
                                 UserTagRelationMapper userTagRelationMapper,
                                 ProfileService profileService,
                                 RecallService recallService,
                                 RankingService rankingService,
                                 RerankService rerankService,
                                 ExplanationService explanationService,
                                 RecommendationTuningContext tuningContext) {
        this.userMapper = userMapper;
        this.tagMapper = tagMapper;
        this.userTagRelationMapper = userTagRelationMapper;
        this.profileService = profileService;
        this.recallService = recallService;
        this.rankingService = rankingService;
        this.rerankService = rerankService;
        this.explanationService = explanationService;
        this.tuningContext = tuningContext;
    }

    @Override
    public EvaluationSummaryVO generateSummary(Integer topK) {
        int effectiveTopK = topK == null || topK < 1 ? 3 : topK;
        String scenarioMode = tuningContext.getScenarioMode();

        List<UserEntity> activeUsers = userMapper.selectList(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getStatus, 1)
        );
        List<TagEntity> activeTags = tagMapper.selectList(
                new LambdaQueryWrapper<TagEntity>().eq(TagEntity::getStatus, 1)
        );
        List<UserTagRelationEntity> relations = userTagRelationMapper.selectList(new LambdaQueryWrapper<>());

        Map<Long, Set<Long>> userTagMap = relations.stream()
                .filter(item -> item.getUserId() != null && item.getTagId() != null)
                .collect(Collectors.groupingBy(
                        UserTagRelationEntity::getUserId,
                        Collectors.mapping(UserTagRelationEntity::getTagId, Collectors.toSet())
                ));

        Map<Long, UserEntity> userCache = new HashMap<>();
        BaselineAccumulator overlap = new BaselineAccumulator("tag_overlap", "标签重叠");
        BaselineAccumulator cosine = new BaselineAccumulator("cosine_similarity", "余弦排序");
        BaselineAccumulator fullNoTrust = new BaselineAccumulator("full_pipeline_no_trust", "完整链路（无可信分）");
        BaselineAccumulator fullWithTrust = new BaselineAccumulator("full_pipeline_with_trust", "完整链路（含可信分）");

        for (UserEntity requestUser : activeUsers) {
            UserProfileModel profileModel = profileService.getProfile(requestUser.getId());
            if (profileModel == null) {
                profileModel = new UserProfileModel();
                profileModel.setUserId(requestUser.getId());
            }
            if (profileModel.getVector().isEmpty()) {
                profileModel = profileService.buildProfile(requestUser.getId(), "evaluation");
            }
            Set<Long> candidateUserIds = defaultIfNull(recallService.recallCandidateUserIds(profileModel), Set.of());
            List<RankingCandidateModel> rankingList = defaultIfNull(
                    rankingService.rank(requestUser.getId(), candidateUserIds),
                    List.of()
            );
            List<RankingCandidateModel> rankingSnapshot = cloneCandidates(rankingList);
            List<RankingCandidateModel> rerankedWithTrust = rerankWithCurrentScenario(requestUser.getId(), rankingList, true);
            List<RankingCandidateModel> rerankedNoTrust = rerankWithCurrentScenario(requestUser.getId(), rankingList, false);

            overlap.addEvaluation(
                    requestUser,
                    sortCandidates(rankingSnapshot, Comparator
                            .comparing(RankingCandidateModel::getRecallScore, Comparator.nullsLast(BigDecimal::compareTo))
                            .reversed()
                            .thenComparing(RankingCandidateModel::getTargetUserId)),
                    candidateUserIds.size(),
                    effectiveTopK,
                    userTagMap,
                    userCache
            );
            cosine.addEvaluation(
                    requestUser,
                    sortCandidates(rankingSnapshot, Comparator
                            .comparing(RankingCandidateModel::getRankScore, Comparator.nullsLast(BigDecimal::compareTo))
                            .reversed()
                            .thenComparing(RankingCandidateModel::getTargetUserId)),
                    candidateUserIds.size(),
                    effectiveTopK,
                    userTagMap,
                    userCache
            );
            fullNoTrust.addEvaluation(
                    requestUser,
                    sortCandidates(rerankedNoTrust, Comparator
                            .comparing(RankingCandidateModel::getFinalScore, Comparator.nullsLast(BigDecimal::compareTo))
                            .reversed()
                            .thenComparing(RankingCandidateModel::getTargetUserId)),
                    candidateUserIds.size(),
                    effectiveTopK,
                    userTagMap,
                    userCache
            );
            fullWithTrust.addEvaluation(
                    requestUser,
                    sortCandidates(rerankedWithTrust, Comparator
                            .comparing(RankingCandidateModel::getFinalScore, Comparator.nullsLast(BigDecimal::compareTo))
                            .reversed()
                            .thenComparing(RankingCandidateModel::getTargetUserId)),
                    candidateUserIds.size(),
                    effectiveTopK,
                    userTagMap,
                    userCache
            );
        }

        EvaluationSummaryVO summary = new EvaluationSummaryVO();
        summary.setGeneratedAt(LocalDateTime.now().toString());
        summary.setTopK(effectiveTopK);
        summary.setScenarioMode(scenarioMode);
        summary.setScenarioLabel(RecommendationScenarioMode.labelOf(scenarioMode));
        summary.setActiveUserCount(activeUsers.size());
        summary.setTagCount(activeTags.size());
        summary.setRelationCount(relations.size());
        summary.setProxyRelevanceRule(PROXY_RULE);
        summary.getBaselines().add(overlap.toVO());
        summary.getBaselines().add(cosine.toVO());
        summary.getBaselines().add(fullNoTrust.toVO());
        summary.getBaselines().add(fullWithTrust.toVO());
        return summary;
    }

    @Override
    public String generateMarkdownReport(Integer topK) {
        EvaluationSummaryVO summary = generateSummary(topK);
        StringBuilder builder = new StringBuilder();
        builder.append("# 推荐评估摘要\n\n");
        builder.append("- 生成时间: ").append(summary.getGeneratedAt()).append('\n');
        builder.append("- 场景模式: ").append(summary.getScenarioMode()).append(" / ").append(summary.getScenarioLabel()).append('\n');
        builder.append("- TopK: ").append(summary.getTopK()).append('\n');
        builder.append("- 活跃用户数: ").append(summary.getActiveUserCount()).append('\n');
        builder.append("- 标签数: ").append(summary.getTagCount()).append('\n');
        builder.append("- 关系数: ").append(summary.getRelationCount()).append('\n');
        builder.append("- 代理相关性规则: ").append(summary.getProxyRelevanceRule()).append("\n\n");
        builder.append("| 基线 | 平均召回候选数 | 平均返回数 | Precision@K | HitRate@K | 解释覆盖率 |\n");
        builder.append("| --- | --- | --- | --- | --- | --- |\n");
        for (EvaluationBaselineVO baseline : summary.getBaselines()) {
            builder.append("| ")
                    .append(baseline.getBaselineName())
                    .append(" | ")
                    .append(formatDecimal(baseline.getAverageRecallCandidateCount()))
                    .append(" | ")
                    .append(formatDecimal(baseline.getAverageTopKReturnCount()))
                    .append(" | ")
                    .append(formatDecimal(baseline.getPrecisionAtK()))
                    .append(" | ")
                    .append(formatDecimal(baseline.getHitRateAtK()))
                    .append(" | ")
                    .append(formatDecimal(baseline.getExplanationPresenceRate()))
                    .append(" |\n");
        }
        return builder.toString();
    }

    private List<RankingCandidateModel> rerankWithCurrentScenario(Long requestUserId,
                                                                  List<RankingCandidateModel> rankingList,
                                                                  boolean trustEnabled) {
        try (RecommendationTuningContext.Scope ignored = tuningContext.withOverrides(
                null,
                null,
                tuningContext.getScenarioMode(),
                trustEnabled
        )) {
            return defaultIfNull(rerankService.rerank(requestUserId, cloneCandidates(rankingList)), List.of());
        }
    }

    private List<RankingCandidateModel> sortCandidates(List<RankingCandidateModel> candidates,
                                                       Comparator<RankingCandidateModel> comparator) {
        return candidates.stream().sorted(comparator).toList();
    }

    private List<RankingCandidateModel> cloneCandidates(List<RankingCandidateModel> candidates) {
        List<RankingCandidateModel> copies = new ArrayList<>();
        for (RankingCandidateModel candidate : candidates) {
            RankingCandidateModel copy = new RankingCandidateModel();
            copy.setTargetUserId(candidate.getTargetUserId());
            copy.setRecallScore(candidate.getRecallScore());
            copy.setRankScore(candidate.getRankScore());
            copy.setInterestScore(candidate.getInterestScore());
            copy.setRerankScore(candidate.getRerankScore());
            copy.setCampusScore(candidate.getCampusScore());
            copy.setTrustScore(candidate.getTrustScore());
            copy.setFinalScore(candidate.getFinalScore());
            copy.setScenarioMode(candidate.getScenarioMode());
            copy.setScenarioLabel(candidate.getScenarioLabel());
            copy.setTrustReasons(new ArrayList<>(candidate.getTrustReasons()));
            copy.setContributions(new ArrayList<>(candidate.getContributions()));
            copy.setRuleHits(new ArrayList<>(candidate.getRuleHits()));
            copies.add(copy);
        }
        return copies;
    }

    private <T> T defaultIfNull(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String formatDecimal(BigDecimal value) {
        return value == null ? "0.0000" : value.setScale(4, RoundingMode.HALF_UP).toPlainString();
    }

    private final class BaselineAccumulator {
        private final String baselineCode;
        private final String baselineName;
        private int evaluatedUserCount;
        private int totalRecallCandidateCount;
        private int totalTopKReturnCount;
        private int totalRelevantCount;
        private int totalHitCount;
        private int totalExplanationPresentCount;

        private BaselineAccumulator(String baselineCode, String baselineName) {
            this.baselineCode = baselineCode;
            this.baselineName = baselineName;
        }

        private void addEvaluation(UserEntity requestUser,
                                   List<RankingCandidateModel> sortedCandidates,
                                   int recallCandidateCount,
                                   int topK,
                                   Map<Long, Set<Long>> userTagMap,
                                   Map<Long, UserEntity> userCache) {
            evaluatedUserCount++;
            totalRecallCandidateCount += recallCandidateCount;
            List<RankingCandidateModel> topItems = sortedCandidates.stream().limit(topK).toList();
            totalTopKReturnCount += topItems.size();

            int relevantCount = 0;
            int explanationPresent = 0;
            for (RankingCandidateModel candidate : topItems) {
                UserEntity targetUser = userCache.computeIfAbsent(candidate.getTargetUserId(), userMapper::selectById);
                if (isProxyRelevant(requestUser, targetUser, userTagMap)) {
                    relevantCount++;
                }
                var explanation = explanationService.generate(candidate);
                String reasonText = explanation == null ? null : explanation.getReasonText();
                if (reasonText != null && !reasonText.isBlank()) {
                    explanationPresent++;
                }
            }
            totalRelevantCount += relevantCount;
            totalExplanationPresentCount += explanationPresent;
            if (relevantCount > 0) {
                totalHitCount++;
            }
        }

        private EvaluationBaselineVO toVO() {
            EvaluationBaselineVO baseline = new EvaluationBaselineVO();
            baseline.setBaselineCode(baselineCode);
            baseline.setBaselineName(baselineName);
            baseline.setEvaluatedUserCount(evaluatedUserCount);
            baseline.setAverageRecallCandidateCount(average(totalRecallCandidateCount, evaluatedUserCount));
            baseline.setAverageTopKReturnCount(average(totalTopKReturnCount, evaluatedUserCount));
            baseline.setPrecisionAtK(average(totalRelevantCount, totalTopKReturnCount));
            baseline.setHitRateAtK(average(totalHitCount, evaluatedUserCount));
            baseline.setExplanationPresenceRate(average(totalExplanationPresentCount, totalTopKReturnCount));
            return baseline;
        }

        private BigDecimal average(int numerator, int denominator) {
            if (denominator <= 0) {
                return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            }
            return BigDecimal.valueOf(numerator)
                    .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
        }

        private boolean isProxyRelevant(UserEntity requestUser,
                                        UserEntity targetUser,
                                        Map<Long, Set<Long>> userTagMap) {
            if (requestUser == null || targetUser == null || requestUser.getId() == null || targetUser.getId() == null) {
                return false;
            }
            Set<Long> requestTags = userTagMap.getOrDefault(requestUser.getId(), Set.of());
            Set<Long> targetTags = userTagMap.getOrDefault(targetUser.getId(), Set.of());
            long sharedTagCount = requestTags.stream().filter(targetTags::contains).count();
            boolean sameMajor = requestUser.getMajor() != null
                    && requestUser.getMajor().equalsIgnoreCase(targetUser.getMajor());
            return sharedTagCount >= 2 || (sameMajor && sharedTagCount >= 1);
        }
    }
}
