package com.lcj.campusreco.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.lcj.campusreco.config.RecommendationTuningContext;
import com.lcj.campusreco.domain.vo.EvaluationBaselineVO;
import com.lcj.campusreco.domain.vo.EvaluationScenarioExportVO;
import com.lcj.campusreco.domain.vo.EvaluationSummaryVO;
import com.lcj.campusreco.service.EvaluationService;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationScenarioMatrixServiceImplTest {

    @Mock
    private EvaluationService evaluationService;

    @Test
    void exportScenarioMatrixWritesMarkdownForScenarioCombinations() throws Exception {
        Path exportDir = Path.of("target/test-generated-docs-scenarios");
        Files.createDirectories(exportDir);
        Files.deleteIfExists(exportDir.resolve("recommendation-scenario-matrix-latest.md"));

        when(evaluationService.generateSummary(3)).thenReturn(createSummary(3, "0.9444", "0.8888"));

        EvaluationScenarioMatrixServiceImpl service = new EvaluationScenarioMatrixServiceImpl(
                evaluationService,
                new RecommendationTuningContext(5, BigDecimal.ONE, "interest_partner", true),
                exportDir.toString()
        );

        EvaluationScenarioExportVO export = service.exportScenarioMatrix(
                List.of("study_partner", "interest_partner"),
                List.of(3),
                List.of(3, 5),
                List.of(new BigDecimal("0.8"), new BigDecimal("1.0"))
        );

        assertEquals(8, export.getScenarioCount());
        assertEquals(List.of("interest_partner", "study_partner"), export.getScenarioModes());
        assertEquals(2, export.getProfileTopTagCounts().size());
        assertEquals(2, export.getRerankWeightScales().size());
        assertTrue(Files.exists(Path.of(export.getFilePath())));

        String markdown = Files.readString(Path.of(export.getFilePath()));
        assertTrue(markdown.contains("# 推荐场景参数矩阵"));
        assertTrue(markdown.contains("| study_partner | 3 | 3 | 0.8 |"));
        assertTrue(markdown.contains("| interest_partner | 3 | 5 | 1.0 |"));
    }

    private EvaluationSummaryVO createSummary(int topK, String improvedTfIdfPrecision, String fullPipelinePrecision) {
        EvaluationSummaryVO summary = new EvaluationSummaryVO();
        summary.setGeneratedAt("2026-04-20T23:50:00");
        summary.setTopK(topK);
        summary.setActiveUserCount(12);

        EvaluationBaselineVO improvedTfIdf = new EvaluationBaselineVO();
        improvedTfIdf.setBaselineCode("a4_improved_tfidf");
        improvedTfIdf.setBaselineName("A4 改进 TF-IDF 画像算法");
        improvedTfIdf.setPrecisionAtK(new BigDecimal(improvedTfIdfPrecision));
        improvedTfIdf.setNdcgAtK(new BigDecimal("0.8000"));
        improvedTfIdf.setHitRateAtK(new BigDecimal("0.7500"));
        improvedTfIdf.setCoverageRate(new BigDecimal("0.6667"));
        improvedTfIdf.setExplanationPresenceRate(new BigDecimal("1.0000"));

        EvaluationBaselineVO fullPipeline = new EvaluationBaselineVO();
        fullPipeline.setBaselineCode("a5_improved_tfidf_with_scene_rerank");
        fullPipeline.setBaselineName("A5 改进 TF-IDF + 场景规则重排");
        fullPipeline.setPrecisionAtK(new BigDecimal(fullPipelinePrecision));
        fullPipeline.setNdcgAtK(new BigDecimal("0.9000"));
        fullPipeline.setHitRateAtK(new BigDecimal("1.0000"));
        fullPipeline.setCoverageRate(new BigDecimal("0.7500"));
        fullPipeline.setExplanationPresenceRate(new BigDecimal("1.0000"));

        summary.getBaselines().add(improvedTfIdf);
        summary.getBaselines().add(fullPipeline);
        return summary;
    }
}
