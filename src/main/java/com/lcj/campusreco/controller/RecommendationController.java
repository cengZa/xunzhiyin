package com.lcj.campusreco.controller;

import com.lcj.campusreco.common.api.ApiResponse;
import com.lcj.campusreco.domain.dto.RecommendRequestDTO;
import com.lcj.campusreco.domain.vo.ExplanationVO;
import com.lcj.campusreco.domain.vo.RecommendationDetailVO;
import com.lcj.campusreco.service.ExplanationService;
import com.lcj.campusreco.service.RecommendationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final ExplanationService explanationService;

    public RecommendationController(RecommendationService recommendationService,
                                    ExplanationService explanationService) {
        this.recommendationService = recommendationService;
        this.explanationService = explanationService;
    }

    @GetMapping("/{userId}")
    public ApiResponse<RecommendationDetailVO> recommend(@PathVariable Long userId,
                                                         @RequestParam(defaultValue = "10") Integer topK,
                                                         @RequestParam(defaultValue = "true") Boolean useCache) {
        RecommendRequestDTO dto = new RecommendRequestDTO();
        dto.setUserId(userId);
        dto.setTopK(topK);
        dto.setUseCache(useCache);
        return ApiResponse.success(recommendationService.recommend(dto));
    }

    @GetMapping("/{userId}/detail")
    public ApiResponse<RecommendationDetailVO> getRecommendationDetail(@PathVariable Long userId) {
        return ApiResponse.success(recommendationService.getRecommendationDetail(userId));
    }

    @GetMapping("/{recommendationId}/explanation")
    public ApiResponse<ExplanationVO> getExplanation(@PathVariable Long recommendationId) {
        return ApiResponse.success(explanationService.getByRecommendationId(recommendationId));
    }
}
