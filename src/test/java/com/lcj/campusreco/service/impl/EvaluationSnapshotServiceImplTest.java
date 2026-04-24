package com.lcj.campusreco.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.lcj.campusreco.domain.vo.EvaluationBaselineVO;
import com.lcj.campusreco.domain.vo.EvaluationSummaryVO;
import com.lcj.campusreco.domain.vo.EvaluationExportVO;
import com.lcj.campusreco.service.EvaluationService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationSnapshotServiceImplTest {

    @Mock
    private EvaluationService evaluationService;

    @Test
    void exportLatestReportWritesMarkdownSnapshotToConfiguredDirectory() throws Exception {
        Path exportDir = Path.of("target/test-generated-docs-snapshot");
        Files.createDirectories(exportDir);
        Files.deleteIfExists(exportDir.resolve("recommendation-evaluation-latest.md"));

        EvaluationSummaryVO summary = new EvaluationSummaryVO();
        summary.setGeneratedAt("2026-04-12T12:00:00");
        summary.setTopK(3);
        summary.getBaselines().add(new EvaluationBaselineVO());
        summary.getBaselines().add(new EvaluationBaselineVO());
        summary.getBaselines().add(new EvaluationBaselineVO());
        when(evaluationService.generateSummary(3)).thenReturn(summary);
        when(evaluationService.generateMarkdownReport(3)).thenReturn("# 推荐评估摘要\n");

        EvaluationSnapshotServiceImpl snapshotService =
                new EvaluationSnapshotServiceImpl(evaluationService, exportDir.toString());

        EvaluationExportVO export = snapshotService.exportLatestReport(3);

        assertEquals(3, export.getTopK());
        assertEquals(3, export.getBaselineCount());
        assertTrue(export.getFileName().endsWith(".md"));
        assertTrue(Files.exists(Path.of(export.getFilePath())));
        assertTrue(Files.readString(Path.of(export.getFilePath())).contains("推荐评估摘要"));

        Files.deleteIfExists(Path.of(export.getFilePath()));
    }
}
