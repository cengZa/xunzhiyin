package com.lcj.campusreco.service.impl;

import com.lcj.campusreco.common.constant.RecommendationScenarioMode;
import com.lcj.campusreco.config.RecommendationTuningContext;
import com.lcj.campusreco.domain.vo.EvaluationBaselineVO;
import com.lcj.campusreco.domain.vo.EvaluationScenarioExportVO;
import com.lcj.campusreco.domain.vo.EvaluationSummaryVO;
import com.lcj.campusreco.service.EvaluationScenarioMatrixService;
import com.lcj.campusreco.service.EvaluationService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EvaluationScenarioMatrixServiceImpl implements EvaluationScenarioMatrixService {

    private static final String FILE_NAME = "recommendation-scenario-matrix-latest.md";

    private final EvaluationService evaluationService;
    private final RecommendationTuningContext tuningContext;
    private final String generatedDocsDir;

    public EvaluationScenarioMatrixServiceImpl(EvaluationService evaluationService,
                                               RecommendationTuningContext tuningContext,
                                               @Value("${app.generated-docs-dir:docs/generated}") String generatedDocsDir) {
        this.evaluationService = evaluationService;
        this.tuningContext = tuningContext;
        this.generatedDocsDir = generatedDocsDir;
    }

    @Override
    public EvaluationScenarioExportVO exportScenarioMatrix(List<String> scenarioModes,
                                                           List<Integer> topKValues,
                                                           List<Integer> profileTopTagCounts,
                                                           List<BigDecimal> rerankWeightScales) {
        List<String> normalizedModes = normalizeScenarioModes(scenarioModes);
        List<Integer> normalizedTopKs = normalizePositiveIntegers(topKValues, List.of(3, 5));
        List<Integer> normalizedTagCounts = normalizePositiveIntegers(profileTopTagCounts, List.of(3, 5));
        List<BigDecimal> normalizedWeightScales = normalizePositiveDecimals(
                rerankWeightScales,
                List.of(new BigDecimal("0.8"), BigDecimal.ONE, new BigDecimal("1.2"))
        );

        List<ScenarioRow> scenarioRows = new ArrayList<>();
        for (String scenarioMode : normalizedModes) {
            for (Integer topK : normalizedTopKs) {
                for (Integer profileTopTagCount : normalizedTagCounts) {
                    for (BigDecimal rerankWeightScale : normalizedWeightScales) {
                        try (RecommendationTuningContext.Scope ignored =
                                     tuningContext.withOverrides(profileTopTagCount, rerankWeightScale, scenarioMode, true)) {
                            EvaluationSummaryVO summary = evaluationService.generateSummary(topK);
                            scenarioRows.add(ScenarioRow.from(
                                    scenarioMode,
                                    summary,
                                    profileTopTagCount,
                                    rerankWeightScale
                            ));
                        }
                    }
                }
            }
        }

        Path outputDir = Path.of(generatedDocsDir);
        Path outputFile = outputDir.resolve(FILE_NAME);
        try {
            Files.createDirectories(outputDir);
            Files.writeString(
                    outputFile,
                    buildMarkdown(normalizedModes, normalizedTopKs, normalizedTagCounts, normalizedWeightScales, scenarioRows),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to export scenario matrix to " + outputFile.toAbsolutePath(), ex);
        }

        EvaluationScenarioExportVO exportVO = new EvaluationScenarioExportVO();
        exportVO.setFileName(FILE_NAME);
        exportVO.setFilePath(outputFile.toAbsolutePath().toString());
        exportVO.setScenarioCount(scenarioRows.size());
        exportVO.getScenarioModes().addAll(normalizedModes);
        exportVO.getTopKValues().addAll(normalizedTopKs);
        exportVO.getProfileTopTagCounts().addAll(normalizedTagCounts);
        exportVO.getRerankWeightScales().addAll(normalizedWeightScales);
        return exportVO;
    }

    private String buildMarkdown(List<String> scenarioModes,
                                 List<Integer> topKValues,
                                 List<Integer> profileTopTagCounts,
                                 List<BigDecimal> rerankWeightScales,
                                 List<ScenarioRow> rows) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 推荐场景参数矩阵\n\n");
        builder.append("- 场景模式集合: ").append(scenarioModes).append('\n');
        builder.append("- TopK 集合: ").append(topKValues).append('\n');
        builder.append("- 画像 Top 标签数集合: ").append(profileTopTagCounts).append('\n');
        builder.append("- 重排权重缩放集合: ").append(rerankWeightScales).append("\n\n");
        builder.append("| 场景模式 | TopK | 画像 Top 标签数 | 重排权重缩放 | 完整链路（无可信分）Precision@K | 完整链路（含可信分）Precision@K | Precision 增益 | HitRate | 解释覆盖率 |\n");
        builder.append("| --- | --- | --- | --- | --- | --- | --- | --- | --- |\n");
        for (ScenarioRow row : rows) {
            builder.append("| ")
                    .append(row.scenarioMode)
                    .append(" | ")
                    .append(row.topK)
                    .append(" | ")
                    .append(row.profileTopTagCount)
                    .append(" | ")
                    .append(formatScale(row.rerankWeightScale))
                    .append(" | ")
                    .append(format(row.noTrustPrecision))
                    .append(" | ")
                    .append(format(row.withTrustPrecision))
                    .append(" | ")
                    .append(format(row.precisionGain))
                    .append(" | ")
                    .append(format(row.withTrustHitRate))
                    .append(" | ")
                    .append(format(row.withTrustExplanationRate))
                    .append(" |\n");
        }
        return builder.toString();
    }

    private List<String> normalizeScenarioModes(List<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        if (values != null) {
            values.stream()
                    .map(RecommendationScenarioMode::normalize)
                    .sorted()
                    .forEach(normalized::add);
        }
        if (normalized.isEmpty()) {
            normalized.addAll(RecommendationScenarioMode.allModes());
        }
        return normalized.stream().sorted(Comparator.naturalOrder()).toList();
    }

    private List<Integer> normalizePositiveIntegers(List<Integer> values, List<Integer> defaults) {
        Set<Integer> normalized = new LinkedHashSet<>();
        if (values != null) {
            values.stream()
                    .filter(value -> value != null && value > 0)
                    .sorted()
                    .forEach(normalized::add);
        }
        if (normalized.isEmpty()) {
            normalized.addAll(defaults);
        }
        return normalized.stream().sorted(Comparator.naturalOrder()).toList();
    }

    private List<BigDecimal> normalizePositiveDecimals(List<BigDecimal> values, List<BigDecimal> defaults) {
        Set<BigDecimal> normalized = new LinkedHashSet<>();
        if (values != null) {
            values.stream()
                    .filter(value -> value != null && value.signum() > 0)
                    .map(value -> value.setScale(1, RoundingMode.HALF_UP))
                    .sorted()
                    .forEach(normalized::add);
        }
        if (normalized.isEmpty()) {
            normalized.addAll(defaults);
        }
        return normalized.stream().sorted().toList();
    }

    private String format(BigDecimal value) {
        return value == null ? "0.0000" : value.setScale(4, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatScale(BigDecimal value) {
        return value == null ? "1.0" : value.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    private record ScenarioRow(String scenarioMode,
                               int topK,
                               int profileTopTagCount,
                               BigDecimal rerankWeightScale,
                               BigDecimal noTrustPrecision,
                               BigDecimal withTrustPrecision,
                               BigDecimal precisionGain,
                               BigDecimal withTrustHitRate,
                               BigDecimal withTrustExplanationRate) {

        private static ScenarioRow from(String scenarioMode,
                                        EvaluationSummaryVO summary,
                                        Integer profileTopTagCount,
                                        BigDecimal rerankWeightScale) {
            EvaluationBaselineVO noTrust = findBaseline(summary, "full_pipeline_no_trust");
            EvaluationBaselineVO withTrust = findBaseline(summary, "full_pipeline_with_trust");
            BigDecimal noTrustPrecision = valueOf(noTrust == null ? null : noTrust.getPrecisionAtK());
            BigDecimal withTrustPrecision = valueOf(withTrust == null ? null : withTrust.getPrecisionAtK());
            return new ScenarioRow(
                    RecommendationScenarioMode.normalize(scenarioMode),
                    summary.getTopK(),
                    profileTopTagCount,
                    rerankWeightScale,
                    noTrustPrecision,
                    withTrustPrecision,
                    withTrustPrecision.subtract(noTrustPrecision),
                    valueOf(withTrust == null ? null : withTrust.getHitRateAtK()),
                    valueOf(withTrust == null ? null : withTrust.getExplanationPresenceRate())
            );
        }

        private static EvaluationBaselineVO findBaseline(EvaluationSummaryVO summary, String code) {
            return summary.getBaselines().stream()
                    .filter(item -> code.equals(item.getBaselineCode()))
                    .findFirst()
                    .orElse(null);
        }

        private static BigDecimal valueOf(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    }
}
