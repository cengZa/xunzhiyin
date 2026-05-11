package com.lcj.campusreco.controller;

import com.lcj.campusreco.common.api.ApiResponse;
import com.lcj.campusreco.domain.vo.DemoComparisonVO;
import com.lcj.campusreco.domain.vo.DemoPipelineVO;
import com.lcj.campusreco.domain.vo.DemoStoryVO;
import com.lcj.campusreco.domain.vo.EvaluationExportVO;
import com.lcj.campusreco.domain.vo.EvaluationMatrixExportVO;
import com.lcj.campusreco.domain.vo.EvaluationScenarioExportVO;
import com.lcj.campusreco.domain.vo.EvaluationSummaryVO;
import com.lcj.campusreco.domain.vo.ScalabilityEvaluationExportVO;
import com.lcj.campusreco.service.DemoComparisonService;
import com.lcj.campusreco.service.DemoPipelineService;
import com.lcj.campusreco.service.DemoStoryService;
import com.lcj.campusreco.service.EvaluationMatrixService;
import com.lcj.campusreco.service.EvaluationScenarioMatrixService;
import com.lcj.campusreco.service.EvaluationService;
import com.lcj.campusreco.service.EvaluationSnapshotService;
import com.lcj.campusreco.service.MockDataService;
import com.lcj.campusreco.service.ScalabilityEvaluationService;
import java.math.BigDecimal;
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
    private final DemoComparisonService demoComparisonService;
    private final DemoPipelineService demoPipelineService;
    private final DemoStoryService demoStoryService;
    private final EvaluationService evaluationService;
    private final EvaluationSnapshotService evaluationSnapshotService;
    private final EvaluationMatrixService evaluationMatrixService;
    private final EvaluationScenarioMatrixService evaluationScenarioMatrixService;
    private final ScalabilityEvaluationService scalabilityEvaluationService;

    public AdminController(MockDataService mockDataService,
                           DemoComparisonService demoComparisonService,
                           DemoPipelineService demoPipelineService,
                           DemoStoryService demoStoryService,
                           EvaluationService evaluationService,
                           EvaluationSnapshotService evaluationSnapshotService,
                           EvaluationMatrixService evaluationMatrixService,
                           EvaluationScenarioMatrixService evaluationScenarioMatrixService,
                           ScalabilityEvaluationService scalabilityEvaluationService) {
        this.mockDataService = mockDataService;
        this.demoComparisonService = demoComparisonService;
        this.demoPipelineService = demoPipelineService;
        this.demoStoryService = demoStoryService;
        this.evaluationService = evaluationService;
        this.evaluationSnapshotService = evaluationSnapshotService;
        this.evaluationMatrixService = evaluationMatrixService;
        this.evaluationScenarioMatrixService = evaluationScenarioMatrixService;
        this.scalabilityEvaluationService = scalabilityEvaluationService;
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

    @GetMapping("/demo/story")
    public ApiResponse<DemoStoryVO> getDemoStory(@RequestParam(defaultValue = "interest_partner") String scenarioMode) {
        return ApiResponse.success(demoStoryService.getDefaultStory(scenarioMode));
    }

    @GetMapping("/demo/compare")
    public ApiResponse<DemoComparisonVO> getDemoComparison(@RequestParam(defaultValue = "2001") Long userId,
                                                           @RequestParam(defaultValue = "3") Integer topK,
                                                           @RequestParam(defaultValue = "interest_partner")
                                                           String scenarioMode) {
        return ApiResponse.success(demoComparisonService.compareViews(userId, topK, scenarioMode));
    }

    @GetMapping("/demo/pipeline")
    public ApiResponse<DemoPipelineVO> getDemoPipeline(@RequestParam(defaultValue = "2001") Long userId,
                                                       @RequestParam(defaultValue = "3") Integer topK,
                                                       @RequestParam(defaultValue = "interest_partner")
                                                       String scenarioMode) {
        return ApiResponse.success(demoPipelineService.buildPipeline(userId, topK, scenarioMode));
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

    @PostMapping("/evaluation/scenarios/export")
    public ApiResponse<EvaluationScenarioExportVO> exportEvaluationScenarios(
            @RequestParam(required = false) List<String> scenarioModes,
            @RequestParam(required = false) List<Integer> topKs,
            @RequestParam(required = false) List<Integer> profileTopTagCounts,
            @RequestParam(required = false) List<BigDecimal> rerankWeightScales) {
        return ApiResponse.success(
                evaluationScenarioMatrixService.exportScenarioMatrix(
                        scenarioModes,
                        topKs,
                        profileTopTagCounts,
                        rerankWeightScales
                )
        );
    }

    @PostMapping("/evaluation/scalability/export")
    public ApiResponse<ScalabilityEvaluationExportVO> exportScalabilityMatrix(
            @RequestParam(required = false) List<Integer> userCounts,
            @RequestParam(defaultValue = "5") Integer topK,
            @RequestParam(defaultValue = "interest_partner") String scenarioMode) {
        return ApiResponse.success(
                scalabilityEvaluationService.exportScalabilityMatrix(userCounts, topK, scenarioMode)
        );
    }
}
