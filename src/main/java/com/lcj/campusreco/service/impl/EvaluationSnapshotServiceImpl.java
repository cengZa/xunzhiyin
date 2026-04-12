package com.lcj.campusreco.service.impl;

import com.lcj.campusreco.domain.vo.EvaluationExportVO;
import com.lcj.campusreco.domain.vo.EvaluationSummaryVO;
import com.lcj.campusreco.service.EvaluationService;
import com.lcj.campusreco.service.EvaluationSnapshotService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EvaluationSnapshotServiceImpl implements EvaluationSnapshotService {

    private static final String LATEST_FILE_NAME = "recommendation-evaluation-latest.md";

    private final EvaluationService evaluationService;
    private final String generatedDocsDir;

    public EvaluationSnapshotServiceImpl(EvaluationService evaluationService,
                                         @Value("${app.generated-docs-dir:docs/generated}") String generatedDocsDir) {
        this.evaluationService = evaluationService;
        this.generatedDocsDir = generatedDocsDir;
    }

    @Override
    public EvaluationExportVO exportLatestReport(Integer topK) {
        EvaluationSummaryVO summary = evaluationService.generateSummary(topK);
        String markdown = evaluationService.generateMarkdownReport(topK);
        Path outputDir = Path.of(generatedDocsDir);
        Path outputFile = outputDir.resolve(LATEST_FILE_NAME);

        try {
            Files.createDirectories(outputDir);
            Files.writeString(
                    outputFile,
                    markdown,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to export evaluation report to " + outputFile.toAbsolutePath(), ex);
        }

        EvaluationExportVO exportVO = new EvaluationExportVO();
        exportVO.setFileName(LATEST_FILE_NAME);
        exportVO.setFilePath(outputFile.toAbsolutePath().toString());
        exportVO.setTopK(summary.getTopK());
        exportVO.setBaselineCount(summary.getBaselines().size());
        exportVO.setGeneratedAt(summary.getGeneratedAt());
        return exportVO;
    }
}
