package com.lcj.campusreco.controller;

import com.lcj.campusreco.common.api.ApiResponse;
import com.lcj.campusreco.domain.vo.EvaluationExportVO;
import com.lcj.campusreco.domain.vo.EvaluationMatrixExportVO;
import com.lcj.campusreco.domain.vo.EvaluationSummaryVO;
import com.lcj.campusreco.service.EvaluationMatrixService;
import com.lcj.campusreco.service.EvaluationService;
import com.lcj.campusreco.service.EvaluationSnapshotService;
import com.lcj.campusreco.service.MockDataService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final MockDataService mockDataService;
    private final EvaluationService evaluationService;
    private final EvaluationSnapshotService evaluationSnapshotService;
    private final EvaluationMatrixService evaluationMatrixService;

    public AdminController(MockDataService mockDataService,
                           EvaluationService evaluationService,
                           EvaluationSnapshotService evaluationSnapshotService,
                           EvaluationMatrixService evaluationMatrixService) {
        this.mockDataService = mockDataService;
        this.evaluationService = evaluationService;
        this.evaluationSnapshotService = evaluationSnapshotService;
        this.evaluationMatrixService = evaluationMatrixService;
    }

    @PostMapping("/mock/init")
    public ApiResponse<Map<String, Object>> initMockData() {
        return ApiResponse.success(mockDataService.initMockData());
    }

    @PostMapping("/profiles/rebuild-all")
    public ApiResponse<Map<String, Integer>> rebuildAllProfiles() {
        return ApiResponse.success(Map.of("rebuildCount", mockDataService.rebuildAllProfiles()));
    }

    @PostMapping("/recall/rebuild-index")
    public ApiResponse<Map<String, Integer>> rebuildRecallIndex() {
        return ApiResponse.success(Map.of("indexCount", mockDataService.rebuildRecallIndex()));
    }

    @GetMapping("/evaluation/summary")
    public ApiResponse<EvaluationSummaryVO> getEvaluationSummary(@RequestParam(defaultValue = "3") Integer topK) {
        return ApiResponse.success(evaluationService.generateSummary(topK));
    }

    @GetMapping("/evaluation/report")
    public ApiResponse<String> getEvaluationReport(@RequestParam(defaultValue = "3") Integer topK) {
        return ApiResponse.success(evaluationService.generateMarkdownReport(topK));
    }

    @PostMapping("/evaluation/export")
    public ApiResponse<EvaluationExportVO> exportEvaluationReport(@RequestParam(defaultValue = "3") Integer topK) {
        return ApiResponse.success(evaluationSnapshotService.exportLatestReport(topK));
    }

    @PostMapping("/evaluation/experiments/export")
    public ApiResponse<EvaluationMatrixExportVO> exportEvaluationExperiments(
            @RequestParam(required = false) List<Integer> topKs) {
        return ApiResponse.success(evaluationMatrixService.exportTopKMatrix(topKs));
    }
}
