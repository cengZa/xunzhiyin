package com.lcj.campusreco.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcj.campusreco.common.constant.RecommendationScenarioMode;
import com.lcj.campusreco.common.util.VectorUtils;
import com.lcj.campusreco.config.RecommendationTuningContext;
import com.lcj.campusreco.domain.model.ContributionItemModel;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class EvaluationServiceImpl implements EvaluationService {

    private static final String PROXY_RULE = "分级相关性：共同标签、专业相同、年级接近、场景标签类型命中；grade>=2 记为相关";
    private static final int RELEVANCE_THRESHOLD = 2;

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
        Map<Long, String> tagNameMap = activeTags.stream()
                .collect(Collectors.toMap(TagEntity::getId, TagEntity::getTagName, (left, right) -> left));
        Map<Long, String> tagTypeMap = activeTags.stream()
                .collect(Collectors.toMap(
                        TagEntity::getId,
                        item -> item.getTagType() == null ? "" : item.getTagType(),
                        (left, right) -> left
                ));
        Map<Long, Map<Long, BigDecimal>> plainTfIdfVectors = buildPlainTfIdfVectors(activeUsers, userTagMap);

        Map<Long, UserEntity> userCache = new HashMap<>();
        BaselineAccumulator overlap = new BaselineAccumulator("a1_tag_overlap", "A1 标签重叠匹配", activeUsers.size());
        BaselineAccumulator jaccard = new BaselineAccumulator("a2_jaccard_tag_similarity", "A2 Jaccard 标签集合相似度", activeUsers.size());
        BaselineAccumulator plainTfIdf = new BaselineAccumulator("a3_plain_tfidf_cosine", "A3 TF-IDF 画像余弦相似度", activeUsers.size());
        BaselineAccumulator improvedTfIdf = new BaselineAccumulator("a4_improved_tfidf", "A4 改进 TF-IDF 画像算法", activeUsers.size());
        BaselineAccumulator fullPipeline = new BaselineAccumulator("a5_improved_tfidf_with_scene_rerank", "A5 改进 TF-IDF + 场景规则重排", activeUsers.size());

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
            List<RankingCandidateModel> jaccardRanking = buildJaccardRanking(
                    requestUser.getId(),
                    candidateUserIds,
                    userTagMap,
                    tagNameMap
            );
            List<RankingCandidateModel> plainTfIdfRanking = buildPlainTfIdfRanking(
                    requestUser.getId(),
                    candidateUserIds,
                    userTagMap,
                    plainTfIdfVectors,
                    tagNameMap
            );

            overlap.addEvaluation(
                    requestUser,
                    sortCandidates(rankingSnapshot, Comparator
                            .comparing(RankingCandidateModel::getRecallScore, Comparator.nullsLast(BigDecimal::compareTo))
                            .reversed()
                            .thenComparing(RankingCandidateModel::getTargetUserId)),
                    candidateUserIds.size(),
                    effectiveTopK,
                    userTagMap,
                    userCache,
                    tagTypeMap,
                    scenarioMode
            );
            jaccard.addEvaluation(
                    requestUser,
                    sortCandidates(jaccardRanking, Comparator
                            .comparing(RankingCandidateModel::getRankScore, Comparator.nullsLast(BigDecimal::compareTo))
                            .reversed()
                            .thenComparing(RankingCandidateModel::getTargetUserId)),
                    candidateUserIds.size(),
                    effectiveTopK,
                    userTagMap,
                    userCache,
                    tagTypeMap,
                    scenarioMode
            );
            plainTfIdf.addEvaluation(
                    requestUser,
                    sortCandidates(plainTfIdfRanking, Comparator
                            .comparing(RankingCandidateModel::getRankScore, Comparator.nullsLast(BigDecimal::compareTo))
                            .reversed()
                            .thenComparing(RankingCandidateModel::getTargetUserId)),
                    candidateUserIds.size(),
                    effectiveTopK,
                    userTagMap,
                    userCache,
                    tagTypeMap,
                    scenarioMode
            );
            improvedTfIdf.addEvaluation(
                    requestUser,
                    sortCandidates(rankingSnapshot, Comparator
                            .comparing(RankingCandidateModel::getRankScore, Comparator.nullsLast(BigDecimal::compareTo))
                            .reversed()
                            .thenComparing(RankingCandidateModel::getTargetUserId)),
                    candidateUserIds.size(),
                    effectiveTopK,
                    userTagMap,
                    userCache,
                    tagTypeMap,
                    scenarioMode
            );
            fullPipeline.addEvaluation(
                    requestUser,
                    sortCandidates(rerankedWithTrust, Comparator
                            .comparing(RankingCandidateModel::getFinalScore, Comparator.nullsLast(BigDecimal::compareTo))
                            .reversed()
                            .thenComparing(RankingCandidateModel::getTargetUserId)),
                    candidateUserIds.size(),
                    effectiveTopK,
                    userTagMap,
                    userCache,
                    tagTypeMap,
                    scenarioMode
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
        summary.getBaselines().add(jaccard.toVO());
        summary.getBaselines().add(plainTfIdf.toVO());
        summary.getBaselines().add(improvedTfIdf.toVO());
        summary.getBaselines().add(fullPipeline.toVO());
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
        builder.append("| 算法方案 | 平均召回候选数 | 平均返回数 | Precision@K | NDCG@K | 覆盖率 | 平均响应时间/ms | 解释一致性 |\n");
        builder.append("| --- | --- | --- | --- | --- | --- | --- | --- |\n");
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
                    .append(formatDecimal(baseline.getNdcgAtK()))
                    .append(" | ")
                    .append(formatDecimal(baseline.getCoverageRate()))
                    .append(" | ")
                    .append(formatDecimal(baseline.getAverageResponseTimeMs()))
                    .append(" | ")
                    .append(formatDecimal(baseline.getExplanationPresenceRate()))
                    .append(" |\n");
        }
        return builder.toString();
    }

    private Map<Long, Map<Long, BigDecimal>> buildPlainTfIdfVectors(List<UserEntity> activeUsers,
                                                                    Map<Long, Set<Long>> userTagMap) {
        int userCount = activeUsers.size();
        Map<Long, Integer> documentFrequency = new HashMap<>();
        for (Set<Long> tagIds : userTagMap.values()) {
            for (Long tagId : tagIds) {
                documentFrequency.merge(tagId, 1, Integer::sum);
            }
        }
        Map<Long, Map<Long, BigDecimal>> vectors = new HashMap<>();
        for (UserEntity user : activeUsers) {
            Set<Long> tagIds = userTagMap.getOrDefault(user.getId(), Set.of());
            if (tagIds.isEmpty()) {
                vectors.put(user.getId(), Map.of());
                continue;
            }
            BigDecimal tf = BigDecimal.ONE.divide(BigDecimal.valueOf(tagIds.size()), 8, RoundingMode.HALF_UP);
            Map<Long, BigDecimal> vector = new HashMap<>();
            for (Long tagId : tagIds) {
                int df = documentFrequency.getOrDefault(tagId, 0);
                double idf = Math.log((userCount + 1.0D) / (df + 1.0D)) + 1.0D;
                vector.put(tagId, tf.multiply(BigDecimal.valueOf(idf)).setScale(8, RoundingMode.HALF_UP));
            }
            vectors.put(user.getId(), vector);
        }
        return vectors;
    }

    private List<RankingCandidateModel> buildJaccardRanking(Long requestUserId,
                                                            Set<Long> candidateUserIds,
                                                            Map<Long, Set<Long>> userTagMap,
                                                            Map<Long, String> tagNameMap) {
        Set<Long> requestTags = userTagMap.getOrDefault(requestUserId, Set.of());
        List<RankingCandidateModel> ranking = new ArrayList<>();
        for (Long candidateUserId : candidateUserIds) {
            Set<Long> targetTags = userTagMap.getOrDefault(candidateUserId, Set.of());
            long sharedCount = countSharedTags(requestTags, targetTags);
            Set<Long> union = new HashSet<>(requestTags);
            union.addAll(targetTags);
            BigDecimal rankScore = union.isEmpty()
                    ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(sharedCount)
                    .divide(BigDecimal.valueOf(union.size()), 4, RoundingMode.HALF_UP);

            RankingCandidateModel candidate = new RankingCandidateModel();
            candidate.setTargetUserId(candidateUserId);
            candidate.setRecallScore(BigDecimal.valueOf(sharedCount));
            candidate.setRankScore(rankScore);
            candidate.setInterestScore(rankScore);
            candidate.setRerankScore(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            candidate.setCampusScore(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            candidate.setTrustScore(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            candidate.setFinalScore(rankScore);
            candidate.setContributions(buildTagSetContributions(requestTags, targetTags, tagNameMap));
            ranking.add(candidate);
        }
        return ranking;
    }

    private List<RankingCandidateModel> buildPlainTfIdfRanking(Long requestUserId,
                                                               Set<Long> candidateUserIds,
                                                               Map<Long, Set<Long>> userTagMap,
                                                               Map<Long, Map<Long, BigDecimal>> vectors,
                                                               Map<Long, String> tagNameMap) {
        Map<Long, BigDecimal> requestVector = vectors.getOrDefault(requestUserId, Map.of());
        Set<Long> requestTags = userTagMap.getOrDefault(requestUserId, Set.of());
        List<RankingCandidateModel> ranking = new ArrayList<>();
        for (Long candidateUserId : candidateUserIds) {
            Map<Long, BigDecimal> targetVector = vectors.getOrDefault(candidateUserId, Map.of());
            RankingCandidateModel candidate = new RankingCandidateModel();
            candidate.setTargetUserId(candidateUserId);
            candidate.setRecallScore(BigDecimal.valueOf(countSharedTags(requestTags, userTagMap.getOrDefault(candidateUserId, Set.of()))));
            BigDecimal rankScore = VectorUtils.cosineSimilarity(requestVector, targetVector).setScale(4, RoundingMode.HALF_UP);
            candidate.setRankScore(rankScore);
            candidate.setInterestScore(rankScore);
            candidate.setRerankScore(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            candidate.setCampusScore(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            candidate.setTrustScore(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            candidate.setFinalScore(rankScore);
            candidate.setContributions(buildContributions(requestVector, targetVector, tagNameMap));
            ranking.add(candidate);
        }
        return ranking;
    }

    private List<ContributionItemModel> buildTagSetContributions(Set<Long> requestTags,
                                                                 Set<Long> targetTags,
                                                                 Map<Long, String> tagNameMap) {
        List<ContributionItemModel> contributions = new ArrayList<>();
        for (Long tagId : requestTags) {
            if (!targetTags.contains(tagId)) {
                continue;
            }
            ContributionItemModel contribution = new ContributionItemModel();
            contribution.setTagId(tagId);
            contribution.setTagName(tagNameMap.getOrDefault(tagId, "tag-" + tagId));
            contribution.setSourceWeight(BigDecimal.ONE);
            contribution.setTargetWeight(BigDecimal.ONE);
            contribution.setContributionScore(BigDecimal.ONE);
            contributions.add(contribution);
        }
        contributions.sort(Comparator.comparing(ContributionItemModel::getTagId));
        return contributions;
    }

    private List<ContributionItemModel> buildContributions(Map<Long, BigDecimal> requestVector,
                                                           Map<Long, BigDecimal> targetVector,
                                                           Map<Long, String> tagNameMap) {
        List<ContributionItemModel> contributions = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : requestVector.entrySet()) {
            BigDecimal targetWeight = targetVector.get(entry.getKey());
            if (targetWeight == null || BigDecimal.ZERO.compareTo(targetWeight) == 0) {
                continue;
            }
            ContributionItemModel contribution = new ContributionItemModel();
            contribution.setTagId(entry.getKey());
            contribution.setTagName(tagNameMap.getOrDefault(entry.getKey(), "tag-" + entry.getKey()));
            contribution.setSourceWeight(entry.getValue());
            contribution.setTargetWeight(targetWeight);
            contribution.setContributionScore(entry.getValue().multiply(targetWeight).setScale(4, RoundingMode.HALF_UP));
            contributions.add(contribution);
        }
        contributions.sort(Comparator.comparing(ContributionItemModel::getContributionScore).reversed());
        return contributions;
    }

    private long countSharedTags(Set<Long> left, Set<Long> right) {
        return left.stream().filter(right::contains).count();
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
        private final int candidateUniverseSize;
        private final Set<Long> coveredUserIds = new HashSet<>();
        private int evaluatedUserCount;
        private int totalRecallCandidateCount;
        private int totalTopKReturnCount;
        private int totalRelevantCount;
        private int totalHitCount;
        private int totalExplanationPresentCount;
        private BigDecimal totalNdcg = BigDecimal.ZERO;
        private long totalElapsedNanos;

        private BaselineAccumulator(String baselineCode, String baselineName, int candidateUniverseSize) {
            this.baselineCode = baselineCode;
            this.baselineName = baselineName;
            this.candidateUniverseSize = candidateUniverseSize;
        }

        private void addEvaluation(UserEntity requestUser,
                                   List<RankingCandidateModel> sortedCandidates,
                                   int recallCandidateCount,
                                   int topK,
                                   Map<Long, Set<Long>> userTagMap,
                                   Map<Long, UserEntity> userCache,
                                   Map<Long, String> tagTypeMap,
                                   String scenarioMode) {
            long startNanos = System.nanoTime();
            try {
                evaluatedUserCount++;
                totalRecallCandidateCount += recallCandidateCount;
                List<RankingCandidateModel> topItems = sortedCandidates.stream().limit(topK).toList();
                totalTopKReturnCount += topItems.size();

                int relevantCount = 0;
                int explanationPresent = 0;
                List<Integer> topGrades = new ArrayList<>();
                for (RankingCandidateModel candidate : topItems) {
                    UserEntity targetUser = userCache.computeIfAbsent(candidate.getTargetUserId(), userMapper::selectById);
                    int relevanceGrade = relevanceGrade(requestUser, targetUser, userTagMap, tagTypeMap, scenarioMode);
                    topGrades.add(relevanceGrade);
                    coveredUserIds.add(candidate.getTargetUserId());
                    if (relevanceGrade >= RELEVANCE_THRESHOLD) {
                        relevantCount++;
                    }
                    var explanation = explanationService.generate(candidate);
                    String reasonText = explanation == null ? null : explanation.getReasonText();
                    if (reasonText != null && !reasonText.isBlank()) {
                        explanationPresent++;
                    }
                }

                List<Integer> idealGrades = sortedCandidates.stream()
                        .map(candidate -> {
                            UserEntity targetUser = userCache.computeIfAbsent(candidate.getTargetUserId(), userMapper::selectById);
                            return relevanceGrade(requestUser, targetUser, userTagMap, tagTypeMap, scenarioMode);
                        })
                        .sorted(Comparator.reverseOrder())
                        .limit(topK)
                        .toList();

                totalRelevantCount += relevantCount;
                totalExplanationPresentCount += explanationPresent;
                totalNdcg = totalNdcg.add(BigDecimal.valueOf(calculateNdcg(topGrades, idealGrades)));
                if (relevantCount > 0) {
                    totalHitCount++;
                }
            } finally {
                totalElapsedNanos += System.nanoTime() - startNanos;
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
            baseline.setNdcgAtK(average(totalNdcg, evaluatedUserCount));
            baseline.setCoverageRate(average(coveredUserIds.size(), Math.max(1, candidateUniverseSize)));
            baseline.setHitRateAtK(average(totalHitCount, evaluatedUserCount));
            baseline.setExplanationPresenceRate(average(totalExplanationPresentCount, totalTopKReturnCount));
            baseline.setAverageResponseTimeMs(averageElapsedMillis());
            return baseline;
        }

        private BigDecimal average(int numerator, int denominator) {
            if (denominator <= 0) {
                return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            }
            return BigDecimal.valueOf(numerator)
                    .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
        }

        private BigDecimal average(BigDecimal numerator, int denominator) {
            if (denominator <= 0) {
                return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            }
            return numerator.divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
        }

        private BigDecimal averageElapsedMillis() {
            if (evaluatedUserCount <= 0) {
                return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            }
            return BigDecimal.valueOf(totalElapsedNanos)
                    .divide(BigDecimal.valueOf(evaluatedUserCount), 8, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(1_000_000), 4, RoundingMode.HALF_UP);
        }

        private int relevanceGrade(UserEntity requestUser,
                                   UserEntity targetUser,
                                   Map<Long, Set<Long>> userTagMap,
                                   Map<Long, String> tagTypeMap,
                                   String scenarioMode) {
            if (requestUser == null || targetUser == null || requestUser.getId() == null || targetUser.getId() == null) {
                return 0;
            }
            Set<Long> requestTags = userTagMap.getOrDefault(requestUser.getId(), Set.of());
            Set<Long> targetTags = userTagMap.getOrDefault(targetUser.getId(), Set.of());
            long sharedTagCount = requestTags.stream().filter(targetTags::contains).count();
            boolean sameMajor = requestUser.getMajor() != null
                    && requestUser.getMajor().equalsIgnoreCase(targetUser.getMajor());
            int grade = 0;
            if (sharedTagCount >= 1) {
                grade++;
            }
            if (sharedTagCount >= 2) {
                grade++;
            }
            if (sameMajor) {
                grade++;
            }
            if (isNearbyGrade(requestUser, targetUser)) {
                grade++;
            }
            if (hasScenarioTagOverlap(requestTags, targetTags, tagTypeMap, scenarioMode)) {
                grade++;
            }
            return grade;
        }

        private boolean isNearbyGrade(UserEntity requestUser, UserEntity targetUser) {
            if (requestUser.getGrade() == null || targetUser.getGrade() == null) {
                return false;
            }
            return Math.abs(requestUser.getGrade() - targetUser.getGrade()) <= 1;
        }

        private boolean hasScenarioTagOverlap(Set<Long> requestTags,
                                              Set<Long> targetTags,
                                              Map<Long, String> tagTypeMap,
                                              String scenarioMode) {
            String expectedType = expectedTagType(scenarioMode);
            if (expectedType.isBlank()) {
                return false;
            }
            for (Long tagId : requestTags) {
                if (targetTags.contains(tagId) && expectedType.equalsIgnoreCase(tagTypeMap.getOrDefault(tagId, ""))) {
                    return true;
                }
            }
            return false;
        }

        private String expectedTagType(String scenarioMode) {
            if (RecommendationScenarioMode.STUDY_PARTNER.equals(scenarioMode)) {
                return "academic";
            }
            if (RecommendationScenarioMode.CLUB_PARTNER.equals(scenarioMode)) {
                return "club";
            }
            if (RecommendationScenarioMode.INTEREST_PARTNER.equals(scenarioMode)) {
                return "hobby";
            }
            return "";
        }

        private double calculateNdcg(List<Integer> actualGrades, List<Integer> idealGrades) {
            double idealDcg = dcg(idealGrades);
            if (idealDcg == 0.0D) {
                return 0.0D;
            }
            return dcg(actualGrades) / idealDcg;
        }

        private double dcg(List<Integer> grades) {
            double result = 0.0D;
            for (int index = 0; index < grades.size(); index++) {
                int grade = grades.get(index);
                result += (Math.pow(2.0D, grade) - 1.0D) / (Math.log(index + 2.0D) / Math.log(2.0D));
            }
            return result;
        }
    }
}
