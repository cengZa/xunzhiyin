package com.lcj.campusreco.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lcj.campusreco.domain.vo.ScalabilityEvaluationExportVO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScalabilityEvaluationServiceImplTest {

    @Test
    void exportScalabilityMatrixWritesDeterministicScaleBenchmark() throws Exception {
        Path exportDir = Path.of("target/test-generated-docs-scalability");
        Files.createDirectories(exportDir);
        Files.deleteIfExists(exportDir.resolve("recommendation-scalability-matrix-latest.md"));

        ScalabilityEvaluationServiceImpl service = new ScalabilityEvaluationServiceImpl(exportDir.toString());

        ScalabilityEvaluationExportVO export = service.exportScalabilityMatrix(List.of(500, 100, 100, 300), 5, "study_partner");

        assertEquals("recommendation-scalability-matrix-latest.md", export.getFileName());
        assertEquals(List.of(100, 300, 500), export.getUserCounts());
        assertEquals(3, export.getExperimentCount());
        assertEquals(5, export.getTopK());
        assertEquals("study_partner", export.getScenarioMode());
        assertTrue(Files.exists(Path.of(export.getFilePath())));

        String markdown = Files.readString(Path.of(export.getFilePath()));
        assertTrue(markdown.contains("# 推荐扩展评估矩阵"));
        assertTrue(markdown.contains("| 用户规模 | 标签数 | 关系数 | TopK | 算法方案 |"));
        assertTrue(markdown.contains("| 100 |"));
        assertTrue(markdown.contains("| 300 |"));
        assertTrue(markdown.contains("| 500 |"));
        assertTrue(markdown.contains("A1 标签重叠匹配"));
        assertTrue(markdown.contains("A2 Jaccard 标签集合相似度"));
        assertTrue(markdown.contains("A3 TF-IDF 画像余弦相似度"));
        assertTrue(markdown.contains("A4 改进 TF-IDF 画像算法"));
        assertTrue(markdown.contains("A5 改进 TF-IDF + 场景规则重排"));
    }
}
