package com.lcj.campusreco.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.lcj.campusreco.domain.vo.EvaluationBaselineVO;
import com.lcj.campusreco.domain.vo.EvaluationMatrixExportVO;
import com.lcj.campusreco.domain.vo.EvaluationSummaryVO;
import com.lcj.campusreco.service.EvaluationService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationMatrixServiceImplTest {

    @Mock
    private EvaluationService evaluationService;

    @Test
    void exportTopKMatrixWritesComparisonMarkdownForMultipleTopKValues() throws Exception {
        Path exportDir = Path.of("target/test-generated-docs-matrix");
        Files.createDirectories(exportDir);
        Files.deleteIfExists(exportDir.resolve("recommendation-evaluation-matrix-latest.md"));

        when(evaluationService.generateSummary(3)).thenReturn(createSummary(3, "0.9444"));
        when(evaluationService.generateSummary(5)).thenReturn(createSummary(5, "0.9000"));

        EvaluationMatrixServiceImpl matrixService =
                new EvaluationMatrixServiceImpl(evaluationService, exportDir.toString());

        EvaluationMatrixExportVO export = matrixService.exportTopKMatrix(List.of(3, 5));

        assertEquals(2, export.getExperimentCount());
        assertEquals(2, export.getTopKValues().size());
        assertTrue(export.getFileName().endsWith(".md"));
        assertTrue(Files.exists(Path.of(export.getFilePath())));

        String markdown = Files.readString(Path.of(export.getFilePath()));
        assertTrue(markdown.contains("# Recommendation Evaluation Matrix"));
        assertTrue(markdown.contains("| 3 |"));
        assertTrue(markdown.contains("| 5 |"));
    }

    private EvaluationSummaryVO createSummary(int topK, String precisionAtK) {
        EvaluationSummaryVO summary = new EvaluationSummaryVO();
        summary.setGeneratedAt("2026-04-12T12:45:00");
        summary.setTopK(topK);
        summary.setActiveUserCount(12);
        summary.setTagCount(12);
        summary.setRelationCount(48);

        EvaluationBaselineVO fullPipeline = new EvaluationBaselineVO();
        fullPipeline.setBaselineCode("full_pipeline");
        fullPipeline.setBaselineName("Full Pipeline");
        fullPipeline.setPrecisionAtK(new java.math.BigDecimal(precisionAtK));
        fullPipeline.setHitRateAtK(new java.math.BigDecimal("1.0000"));
        fullPipeline.setExplanationPresenceRate(new java.math.BigDecimal("1.0000"));
        fullPipeline.setAverageRecallCandidateCount(new java.math.BigDecimal("7.6667"));
        fullPipeline.setAverageTopKReturnCount(new java.math.BigDecimal(topK));
        summary.getBaselines().add(fullPipeline);
        return summary;
    }
}
