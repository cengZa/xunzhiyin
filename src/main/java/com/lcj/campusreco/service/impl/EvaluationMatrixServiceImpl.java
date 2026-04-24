package com.lcj.campusreco.service.impl;

import com.lcj.campusreco.domain.vo.EvaluationBaselineVO;
import com.lcj.campusreco.domain.vo.EvaluationMatrixExportVO;
import com.lcj.campusreco.domain.vo.EvaluationSummaryVO;
import com.lcj.campusreco.service.EvaluationMatrixService;
import com.lcj.campusreco.service.EvaluationService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EvaluationMatrixServiceImpl implements EvaluationMatrixService {

    private static final String MATRIX_FILE_NAME = "recommendation-evaluation-matrix-latest.md";

    private final EvaluationService evaluationService;
    private final String generatedDocsDir;

    public EvaluationMatrixServiceImpl(EvaluationService evaluationService,
                                       @Value("${app.generated-docs-dir:docs/generated}") String generatedDocsDir) {
        this.evaluationService = evaluationService;
        this.generatedDocsDir = generatedDocsDir;
    }

    @Override
    public EvaluationMatrixExportVO exportTopKMatrix(List<Integer> topKValues) {
        Set<Integer> normalized = new LinkedHashSet<>();
        if (topKValues != null) {
            for (Integer topKValue : topKValues) {
                if (topKValue != null && topKValue > 0) {
                    normalized.add(topKValue);
                }
            }
        }
        if (normalized.isEmpty()) {
            normalized.add(3);
            normalized.add(5);
            normalized.add(7);
        }

        List<Integer> orderedTopKValues = normalized.stream().sorted(Comparator.naturalOrder()).toList();
        List<EvaluationSummaryVO> summaries = orderedTopKValues.stream()
                .map(evaluationService::generateSummary)
                .toList();

        Path outputDir = Path.of(generatedDocsDir);
        Path outputFile = outputDir.resolve(MATRIX_FILE_NAME);
        try {
            Files.createDirectories(outputDir);
            Files.writeString(
                    outputFile,
                    buildMarkdown(orderedTopKValues, summaries),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to export evaluation matrix to " + outputFile.toAbsolutePath(), ex);
        }

        EvaluationMatrixExportVO exportVO = new EvaluationMatrixExportVO();
        exportVO.setFileName(MATRIX_FILE_NAME);
        exportVO.setFilePath(outputFile.toAbsolutePath().toString());
        exportVO.setExperimentCount(orderedTopKValues.size());
        exportVO.getTopKValues().addAll(orderedTopKValues);
        return exportVO;
    }

    private String buildMarkdown(List<Integer> topKValues, List<EvaluationSummaryVO> summaries) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 推荐评估矩阵\n\n");
        builder.append("- 实验数量: ").append(topKValues.size()).append('\n');
        builder.append("- TopK 集合: ").append(topKValues).append("\n\n");
        builder.append("| TopK | 活跃用户数 | 基线 | Precision@K | HitRate@K | 解释覆盖率 |\n");
        builder.append("| --- | --- | --- | --- | --- | --- |\n");

        for (EvaluationSummaryVO summary : summaries) {
            for (EvaluationBaselineVO baseline : summary.getBaselines()) {
                builder.append("| ")
                        .append(summary.getTopK())
                        .append(" | ")
                        .append(summary.getActiveUserCount())
                        .append(" | ")
                        .append(baseline.getBaselineName())
                        .append(" | ")
                        .append(formatMetric(baseline.getPrecisionAtK()))
                        .append(" | ")
                        .append(formatMetric(baseline.getHitRateAtK()))
                        .append(" | ")
                        .append(formatMetric(baseline.getExplanationPresenceRate()))
                        .append(" |\n");
            }
        }

        return builder.toString();
    }

    private String formatMetric(java.math.BigDecimal value) {
        return value == null ? "0.0000" : value.setScale(4, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
