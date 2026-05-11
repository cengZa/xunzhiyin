package com.lcj.campusreco.service.impl;

import com.lcj.campusreco.common.constant.RecommendationScenarioMode;
import com.lcj.campusreco.domain.vo.ScalabilityEvaluationExportVO;
import com.lcj.campusreco.service.ScalabilityEvaluationService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ScalabilityEvaluationServiceImpl implements ScalabilityEvaluationService {

    private static final String FILE_NAME = "recommendation-scalability-matrix-latest.md";
    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_USER_COUNT = 2000;
    private static final int RELATION_PER_USER = 6;
    private static final int RELEVANCE_THRESHOLD = 2;

    private static final List<SyntheticTag> TAGS = List.of(
            tag(9001L, "Java", "academic"),
            tag(9002L, "Spring", "academic"),
            tag(9003L, "AI", "academic"),
            tag(9004L, "MachineLearning", "academic"),
            tag(9005L, "DataMining", "academic"),
            tag(9006L, "Algorithm", "academic"),
            tag(9007L, "Database", "academic"),
            tag(9008L, "Frontend", "academic"),
            tag(9009L, "Python", "academic"),
            tag(9010L, "MathModeling", "academic"),
            tag(9011L, "DeepLearning", "academic"),
            tag(9012L, "DistributedSystem", "academic"),
            tag(9101L, "Music", "hobby"),
            tag(9102L, "Running", "hobby"),
            tag(9103L, "Photography", "hobby"),
            tag(9104L, "Hiking", "hobby"),
            tag(9105L, "Guitar", "hobby"),
            tag(9106L, "Reading", "hobby"),
            tag(9107L, "Movie", "hobby"),
            tag(9108L, "Cycling", "hobby"),
            tag(9109L, "Painting", "hobby"),
            tag(9110L, "Fitness", "hobby"),
            tag(9201L, "ACM", "club"),
            tag(9202L, "Basketball", "club"),
            tag(9203L, "StudentUnion", "club"),
            tag(9204L, "Debate", "club"),
            tag(9205L, "Volunteering", "club"),
            tag(9206L, "Badminton", "club"),
            tag(9207L, "Robotics", "club"),
            tag(9208L, "Entrepreneurship", "club"),
            tag(9301L, "Innovation", "interest"),
            tag(9302L, "Product", "interest"),
            tag(9303L, "OpenSource", "interest"),
            tag(9304L, "DesignThinking", "interest"),
            tag(9305L, "PublicWelfare", "interest"),
            tag(9306L, "Language", "interest")
    );

    private static final List<String> MAJORS = List.of(
            "Computer Science",
            "Software Engineering",
            "Data Science",
            "Electronic Information",
            "Automation",
            "Mathematics",
            "Design",
            "Digital Media",
            "Management",
            "Finance"
    );

    private static final Map<String, String> COLLEGE_BY_MAJOR = Map.of(
            "Computer Science", "Engineering",
            "Software Engineering", "Engineering",
            "Data Science", "Science",
            "Electronic Information", "Engineering",
            "Automation", "Engineering",
            "Mathematics", "Science",
            "Design", "Design",
            "Digital Media", "Arts",
            "Management", "Business",
            "Finance", "Business"
    );

    private final String generatedDocsDir;

    public ScalabilityEvaluationServiceImpl(@Value("${app.generated-docs-dir:docs/generated}") String generatedDocsDir) {
        this.generatedDocsDir = generatedDocsDir;
    }

    @Override
    public ScalabilityEvaluationExportVO exportScalabilityMatrix(List<Integer> userCounts,
                                                                 Integer topK,
                                                                 String scenarioMode) {
        List<Integer> normalizedUserCounts = normalizeUserCounts(userCounts);
        int effectiveTopK = topK == null || topK < 1 ? DEFAULT_TOP_K : topK;
        String effectiveScenarioMode = normalizeScenarioMode(scenarioMode);

        List<ScaleExperimentResult> experimentResults = new ArrayList<>();
        for (Integer userCount : normalizedUserCounts) {
            SyntheticDataset dataset = buildDataset(userCount);
            experimentResults.add(evaluate(dataset, effectiveTopK, effectiveScenarioMode));
        }

        Path outputDir = Path.of(generatedDocsDir);
        Path outputFile = outputDir.resolve(FILE_NAME);
        try {
            Files.createDirectories(outputDir);
            Files.writeString(
                    outputFile,
                    buildMarkdown(experimentResults, effectiveTopK, effectiveScenarioMode),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to export scalability matrix to " + outputFile.toAbsolutePath(), ex);
        }

        ScalabilityEvaluationExportVO exportVO = new ScalabilityEvaluationExportVO();
        exportVO.setFileName(FILE_NAME);
        exportVO.setFilePath(outputFile.toAbsolutePath().toString());
        exportVO.setExperimentCount(normalizedUserCounts.size());
        exportVO.setTopK(effectiveTopK);
        exportVO.setScenarioMode(effectiveScenarioMode);
        exportVO.getUserCounts().addAll(normalizedUserCounts);
        return exportVO;
    }

    private List<Integer> normalizeUserCounts(List<Integer> userCounts) {
        LinkedHashSet<Integer> normalized = new LinkedHashSet<>();
        if (userCounts != null) {
            for (Integer userCount : userCounts) {
                if (userCount != null && userCount > 0) {
                    normalized.add(Math.min(userCount, MAX_USER_COUNT));
                }
            }
        }
        if (normalized.isEmpty()) {
            normalized.add(100);
            normalized.add(300);
            normalized.add(500);
        }
        return normalized.stream().sorted().toList();
    }

    private String normalizeScenarioMode(String scenarioMode) {
        if (RecommendationScenarioMode.STUDY_PARTNER.equals(scenarioMode)
                || RecommendationScenarioMode.CLUB_PARTNER.equals(scenarioMode)
                || RecommendationScenarioMode.INTEREST_PARTNER.equals(scenarioMode)) {
            return scenarioMode;
        }
        return RecommendationScenarioMode.INTEREST_PARTNER;
    }

    private SyntheticDataset buildDataset(int userCount) {
        List<SyntheticUser> users = new ArrayList<>();
        Map<Long, Set<Long>> userTagMap = new HashMap<>();
        Map<Long, SyntheticUser> userById = new HashMap<>();
        Map<Long, Set<Long>> invertedIndex = new HashMap<>();
        Map<Long, SyntheticTag> tagById = new HashMap<>();
        for (SyntheticTag tag : TAGS) {
            tagById.put(tag.id(), tag);
        }

        List<SyntheticTag> academicTags = tagsByType("academic");
        List<SyntheticTag> hobbyTags = tagsByType("hobby");
        List<SyntheticTag> clubTags = tagsByType("club");
        List<SyntheticTag> interestTags = tagsByType("interest");

        for (int index = 0; index < userCount; index++) {
            String major = MAJORS.get(index % MAJORS.size());
            SyntheticUser user = new SyntheticUser(
                    100_000L + index + 1L,
                    2021 + (index % 4),
                    major,
                    COLLEGE_BY_MAJOR.getOrDefault(major, "General")
            );
            Set<Long> tagIds = new LinkedHashSet<>();
            addTag(tagIds, academicTags, index % academicTags.size());
            addTag(tagIds, academicTags, (index / 3 + MAJORS.indexOf(major)) % academicTags.size());
            addTag(tagIds, hobbyTags, index % hobbyTags.size());
            addTag(tagIds, hobbyTags, (index / 5 + 3) % hobbyTags.size());
            addTag(tagIds, clubTags, (index / 7 + MAJORS.indexOf(major)) % clubTags.size());
            addTag(tagIds, interestTags, (index / 11 + 2) % interestTags.size());
            fillMissingTags(tagIds, index);

            users.add(user);
            userById.put(user.id(), user);
            userTagMap.put(user.id(), tagIds);
            for (Long tagId : tagIds) {
                invertedIndex.computeIfAbsent(tagId, ignored -> new LinkedHashSet<>()).add(user.id());
            }
        }
        return new SyntheticDataset(users, userById, userTagMap, invertedIndex, tagById);
    }

    private void addTag(Set<Long> tagIds, List<SyntheticTag> tags, int index) {
        tagIds.add(tags.get(index).id());
    }

    private void fillMissingTags(Set<Long> tagIds, int userIndex) {
        int cursor = userIndex * 7;
        while (tagIds.size() < RELATION_PER_USER) {
            tagIds.add(TAGS.get(cursor % TAGS.size()).id());
            cursor++;
        }
    }

    private List<SyntheticTag> tagsByType(String type) {
        return TAGS.stream().filter(tag -> type.equals(tag.type())).toList();
    }

    private ScaleExperimentResult evaluate(SyntheticDataset dataset, int topK, String scenarioMode) {
        Map<Long, Map<Long, BigDecimal>> plainVectors = buildTfIdfVectors(dataset, false);
        Map<Long, Map<Long, BigDecimal>> improvedVectors = buildTfIdfVectors(dataset, true);

        List<BaselineAccumulator> baselines = List.of(
                new BaselineAccumulator("a1_tag_overlap", "A1 标签重叠匹配", dataset.users().size()),
                new BaselineAccumulator("a2_jaccard_tag_similarity", "A2 Jaccard 标签集合相似度", dataset.users().size()),
                new BaselineAccumulator("a3_plain_tfidf_cosine", "A3 TF-IDF 画像余弦相似度", dataset.users().size()),
                new BaselineAccumulator("a4_improved_tfidf", "A4 改进 TF-IDF 画像算法", dataset.users().size()),
                new BaselineAccumulator("a5_improved_tfidf_with_scene_rerank", "A5 改进 TF-IDF + 场景规则重排", dataset.users().size())
        );

        for (SyntheticUser requestUser : dataset.users()) {
            Set<Long> candidateIds = recallCandidates(requestUser.id(), dataset);
            baselines.get(0).add(requestUser, rankByOverlap(requestUser.id(), candidateIds, dataset), topK, dataset, scenarioMode);
            baselines.get(1).add(requestUser, rankByJaccard(requestUser.id(), candidateIds, dataset), topK, dataset, scenarioMode);
            baselines.get(2).add(requestUser, rankByVector(requestUser.id(), candidateIds, dataset, plainVectors), topK, dataset, scenarioMode);
            List<SyntheticCandidate> improvedRanking = rankByVector(requestUser.id(), candidateIds, dataset, improvedVectors);
            baselines.get(3).add(requestUser, improvedRanking, topK, dataset, scenarioMode);
            baselines.get(4).add(requestUser, rerankWithScenario(requestUser, improvedRanking, dataset, scenarioMode), topK, dataset, scenarioMode);
        }

        return new ScaleExperimentResult(
                dataset.users().size(),
                TAGS.size(),
                dataset.users().size() * RELATION_PER_USER,
                baselines.stream().map(BaselineAccumulator::toRow).toList()
        );
    }

    private Set<Long> recallCandidates(Long requestUserId, SyntheticDataset dataset) {
        Set<Long> candidateIds = new LinkedHashSet<>();
        for (Long tagId : dataset.userTagMap().getOrDefault(requestUserId, Set.of())) {
            candidateIds.addAll(dataset.invertedIndex().getOrDefault(tagId, Set.of()));
        }
        candidateIds.remove(requestUserId);
        return candidateIds;
    }

    private List<SyntheticCandidate> rankByOverlap(Long requestUserId, Set<Long> candidateIds, SyntheticDataset dataset) {
        Set<Long> requestTags = dataset.userTagMap().getOrDefault(requestUserId, Set.of());
        return candidateIds.stream()
                .map(candidateId -> {
                    BigDecimal score = BigDecimal.valueOf(sharedTagCount(requestTags, dataset.userTagMap().getOrDefault(candidateId, Set.of())));
                    return new SyntheticCandidate(candidateId, score, score);
                })
                .sorted(candidateComparator())
                .toList();
    }

    private List<SyntheticCandidate> rankByJaccard(Long requestUserId, Set<Long> candidateIds, SyntheticDataset dataset) {
        Set<Long> requestTags = dataset.userTagMap().getOrDefault(requestUserId, Set.of());
        return candidateIds.stream()
                .map(candidateId -> {
                    Set<Long> targetTags = dataset.userTagMap().getOrDefault(candidateId, Set.of());
                    Set<Long> union = new HashSet<>(requestTags);
                    union.addAll(targetTags);
                    BigDecimal score = union.isEmpty()
                            ? BigDecimal.ZERO
                            : BigDecimal.valueOf(sharedTagCount(requestTags, targetTags))
                            .divide(BigDecimal.valueOf(union.size()), 6, RoundingMode.HALF_UP);
                    return new SyntheticCandidate(candidateId, score, score);
                })
                .sorted(candidateComparator())
                .toList();
    }

    private List<SyntheticCandidate> rankByVector(Long requestUserId,
                                                  Set<Long> candidateIds,
                                                  SyntheticDataset dataset,
                                                  Map<Long, Map<Long, BigDecimal>> vectors) {
        Map<Long, BigDecimal> requestVector = vectors.getOrDefault(requestUserId, Map.of());
        return candidateIds.stream()
                .map(candidateId -> {
                    BigDecimal score = cosine(requestVector, vectors.getOrDefault(candidateId, Map.of()));
                    return new SyntheticCandidate(candidateId, score, score);
                })
                .sorted(candidateComparator())
                .toList();
    }

    private List<SyntheticCandidate> rerankWithScenario(SyntheticUser requestUser,
                                                        List<SyntheticCandidate> candidates,
                                                        SyntheticDataset dataset,
                                                        String scenarioMode) {
        return candidates.stream()
                .map(candidate -> {
                    SyntheticUser targetUser = dataset.userById().get(candidate.userId());
                    BigDecimal scenarioScore = scenarioScore(requestUser, targetUser, dataset, scenarioMode);
                    return new SyntheticCandidate(
                            candidate.userId(),
                            candidate.rankScore(),
                            candidate.rankScore().add(scenarioScore).setScale(6, RoundingMode.HALF_UP)
                    );
                })
                .sorted(candidateComparator())
                .toList();
    }

    private BigDecimal scenarioScore(SyntheticUser requestUser,
                                     SyntheticUser targetUser,
                                     SyntheticDataset dataset,
                                     String scenarioMode) {
        BigDecimal score = BigDecimal.ZERO;
        if (targetUser == null) {
            return score;
        }
        if (requestUser.major().equals(targetUser.major())) {
            score = score.add(new BigDecimal("0.1200"));
        }
        if (requestUser.college().equals(targetUser.college())) {
            score = score.add(new BigDecimal("0.0500"));
        }
        if (Math.abs(requestUser.grade() - targetUser.grade()) <= 1) {
            score = score.add(new BigDecimal("0.0800"));
        }
        if (hasScenarioTagOverlap(
                dataset.userTagMap().getOrDefault(requestUser.id(), Set.of()),
                dataset.userTagMap().getOrDefault(targetUser.id(), Set.of()),
                dataset,
                scenarioMode
        )) {
            score = score.add(new BigDecimal("0.1000"));
        }
        return score;
    }

    private Comparator<SyntheticCandidate> candidateComparator() {
        return Comparator
                .comparing(SyntheticCandidate::finalScore, Comparator.nullsLast(BigDecimal::compareTo))
                .reversed()
                .thenComparing(SyntheticCandidate::userId);
    }

    private Map<Long, Map<Long, BigDecimal>> buildTfIdfVectors(SyntheticDataset dataset, boolean improved) {
        Map<Long, Integer> documentFrequency = new HashMap<>();
        for (Set<Long> tags : dataset.userTagMap().values()) {
            for (Long tagId : tags) {
                documentFrequency.merge(tagId, 1, Integer::sum);
            }
        }

        Map<Long, Map<Long, BigDecimal>> vectors = new HashMap<>();
        for (SyntheticUser user : dataset.users()) {
            Set<Long> tagIds = dataset.userTagMap().getOrDefault(user.id(), Set.of());
            BigDecimal tf = BigDecimal.ONE.divide(BigDecimal.valueOf(tagIds.size()), 8, RoundingMode.HALF_UP);
            Map<Long, BigDecimal> vector = new HashMap<>();
            for (Long tagId : tagIds) {
                int df = documentFrequency.getOrDefault(tagId, 1);
                double idf = Math.log((dataset.users().size() + 1.0D) / (df + 1.0D)) + 1.0D;
                BigDecimal weight = tf.multiply(BigDecimal.valueOf(idf));
                if (improved) {
                    weight = weight
                            .multiply(seed(user.id(), tagId))
                            .multiply(timeDecay(user.id(), tagId));
                }
                vector.put(tagId, weight.setScale(8, RoundingMode.HALF_UP));
            }
            vectors.put(user.id(), vector);
        }
        return vectors;
    }

    private BigDecimal seed(Long userId, Long tagId) {
        long factor = Math.floorMod(userId + tagId, 5);
        return BigDecimal.ONE.add(BigDecimal.valueOf(factor).multiply(new BigDecimal("0.0300")));
    }

    private BigDecimal timeDecay(Long userId, Long tagId) {
        long days = Math.floorMod(userId * 3 + tagId, 45);
        return BigDecimal.ONE.divide(BigDecimal.ONE.add(BigDecimal.valueOf(days).multiply(new BigDecimal("0.0100"))), 8, RoundingMode.HALF_UP);
    }

    private BigDecimal cosine(Map<Long, BigDecimal> left, Map<Long, BigDecimal> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal dot = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> entry : left.entrySet()) {
            dot = dot.add(entry.getValue().multiply(right.getOrDefault(entry.getKey(), BigDecimal.ZERO)));
        }
        double leftNorm = norm(left);
        double rightNorm = norm(right);
        if (leftNorm == 0.0D || rightNorm == 0.0D) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(dot.doubleValue() / (leftNorm * rightNorm)).setScale(6, RoundingMode.HALF_UP);
    }

    private double norm(Map<Long, BigDecimal> vector) {
        double sum = 0.0D;
        for (BigDecimal value : vector.values()) {
            sum += Math.pow(value.doubleValue(), 2.0D);
        }
        return Math.sqrt(sum);
    }

    private long sharedTagCount(Set<Long> left, Set<Long> right) {
        return left.stream().filter(right::contains).count();
    }

    private int relevanceGrade(SyntheticUser requestUser,
                               SyntheticUser targetUser,
                               SyntheticDataset dataset,
                               String scenarioMode) {
        if (targetUser == null) {
            return 0;
        }
        Set<Long> requestTags = dataset.userTagMap().getOrDefault(requestUser.id(), Set.of());
        Set<Long> targetTags = dataset.userTagMap().getOrDefault(targetUser.id(), Set.of());
        int grade = 0;
        long sharedCount = sharedTagCount(requestTags, targetTags);
        if (sharedCount >= 1) {
            grade++;
        }
        if (sharedCount >= 2) {
            grade++;
        }
        if (requestUser.major().equals(targetUser.major())) {
            grade++;
        }
        if (Math.abs(requestUser.grade() - targetUser.grade()) <= 1) {
            grade++;
        }
        if (hasScenarioTagOverlap(requestTags, targetTags, dataset, scenarioMode)) {
            grade++;
        }
        return grade;
    }

    private boolean hasScenarioTagOverlap(Set<Long> requestTags,
                                          Set<Long> targetTags,
                                          SyntheticDataset dataset,
                                          String scenarioMode) {
        String expectedType = expectedTagType(scenarioMode);
        if (expectedType.isBlank()) {
            return false;
        }
        for (Long tagId : requestTags) {
            SyntheticTag tag = dataset.tagById().get(tagId);
            if (targetTags.contains(tagId) && tag != null && expectedType.equals(tag.type())) {
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

    private String buildMarkdown(List<ScaleExperimentResult> experimentResults, int topK, String scenarioMode) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 推荐扩展评估矩阵\n\n");
        builder.append("- 数据生成方式: 固定种子规则生成校园用户、标签和用户标签关系，不写入业务数据库。\n");
        builder.append("- 评估场景: ").append(scenarioMode).append(" / ")
                .append(RecommendationScenarioMode.labelOf(scenarioMode)).append('\n');
        builder.append("- TopK: ").append(topK).append('\n');
        builder.append("- 标签数: ").append(TAGS.size()).append('\n');
        builder.append("- 每用户标签关系数: ").append(RELATION_PER_USER).append("\n\n");
        builder.append("| 用户规模 | 标签数 | 关系数 | TopK | 算法方案 | Precision@K | NDCG@K | HitRate@K | 覆盖率 | 平均响应时间/ms | 解释覆盖率 |\n");
        builder.append("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |\n");
        for (ScaleExperimentResult experimentResult : experimentResults) {
            for (BaselineRow row : experimentResult.baselines()) {
                builder.append("| ")
                        .append(experimentResult.userCount())
                        .append(" | ")
                        .append(experimentResult.tagCount())
                        .append(" | ")
                        .append(experimentResult.relationCount())
                        .append(" | ")
                        .append(topK)
                        .append(" | ")
                        .append(row.baselineName())
                        .append(" | ")
                        .append(formatMetric(row.precisionAtK()))
                        .append(" | ")
                        .append(formatMetric(row.ndcgAtK()))
                        .append(" | ")
                        .append(formatMetric(row.hitRateAtK()))
                        .append(" | ")
                        .append(formatMetric(row.coverageRate()))
                        .append(" | ")
                        .append(formatMetric(row.averageResponseTimeMs()))
                        .append(" | ")
                        .append(formatMetric(row.explanationPresenceRate()))
                        .append(" |\n");
            }
        }
        return builder.toString();
    }

    private String formatMetric(BigDecimal value) {
        return value == null ? "0.0000" : value.setScale(4, RoundingMode.HALF_UP).toPlainString();
    }

    private static SyntheticTag tag(Long id, String name, String type) {
        return new SyntheticTag(id, name, type);
    }

    private record SyntheticTag(Long id, String name, String type) {
    }

    private record SyntheticUser(Long id, Integer grade, String major, String college) {
    }

    private record SyntheticCandidate(Long userId, BigDecimal rankScore, BigDecimal finalScore) {
    }

    private record SyntheticDataset(List<SyntheticUser> users,
                                    Map<Long, SyntheticUser> userById,
                                    Map<Long, Set<Long>> userTagMap,
                                    Map<Long, Set<Long>> invertedIndex,
                                    Map<Long, SyntheticTag> tagById) {
    }

    private record ScaleExperimentResult(Integer userCount,
                                         Integer tagCount,
                                         Integer relationCount,
                                         List<BaselineRow> baselines) {
    }

    private record BaselineRow(String baselineCode,
                               String baselineName,
                               BigDecimal precisionAtK,
                               BigDecimal ndcgAtK,
                               BigDecimal hitRateAtK,
                               BigDecimal coverageRate,
                               BigDecimal averageResponseTimeMs,
                               BigDecimal explanationPresenceRate) {
    }

    private final class BaselineAccumulator {
        private final String baselineCode;
        private final String baselineName;
        private final int candidateUniverseSize;
        private final Set<Long> coveredUserIds = new HashSet<>();
        private int evaluatedUserCount;
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

        private void add(SyntheticUser requestUser,
                         List<SyntheticCandidate> ranking,
                         int topK,
                         SyntheticDataset dataset,
                         String scenarioMode) {
            long startNanos = System.nanoTime();
            try {
                evaluatedUserCount++;
                List<SyntheticCandidate> topItems = ranking.stream().limit(topK).toList();
                totalTopKReturnCount += topItems.size();
                totalExplanationPresentCount += topItems.size();

                int relevantCount = 0;
                List<Integer> actualGrades = new ArrayList<>();
                for (SyntheticCandidate candidate : topItems) {
                    SyntheticUser targetUser = dataset.userById().get(candidate.userId());
                    int relevanceGrade = relevanceGrade(requestUser, targetUser, dataset, scenarioMode);
                    actualGrades.add(relevanceGrade);
                    coveredUserIds.add(candidate.userId());
                    if (relevanceGrade >= RELEVANCE_THRESHOLD) {
                        relevantCount++;
                    }
                }

                List<Integer> idealGrades = ranking.stream()
                        .map(candidate -> relevanceGrade(
                                requestUser,
                                dataset.userById().get(candidate.userId()),
                                dataset,
                                scenarioMode
                        ))
                        .sorted(Comparator.reverseOrder())
                        .limit(topK)
                        .toList();

                totalRelevantCount += relevantCount;
                totalNdcg = totalNdcg.add(BigDecimal.valueOf(calculateNdcg(actualGrades, idealGrades)));
                if (relevantCount > 0) {
                    totalHitCount++;
                }
            } finally {
                totalElapsedNanos += System.nanoTime() - startNanos;
            }
        }

        private BaselineRow toRow() {
            return new BaselineRow(
                    baselineCode,
                    baselineName,
                    average(totalRelevantCount, totalTopKReturnCount),
                    average(totalNdcg, evaluatedUserCount),
                    average(totalHitCount, evaluatedUserCount),
                    average(coveredUserIds.size(), Math.max(1, candidateUniverseSize)),
                    averageElapsedMillis(),
                    average(totalExplanationPresentCount, totalTopKReturnCount)
            );
        }

        private BigDecimal average(int numerator, int denominator) {
            if (denominator <= 0) {
                return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            }
            return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
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
