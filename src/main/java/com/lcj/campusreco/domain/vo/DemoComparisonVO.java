package com.lcj.campusreco.domain.vo;

import lombok.Data;

@Data
public class DemoComparisonVO {

    private Long userId;
    private Integer topK;
    private Integer candidateCount;
    private String scenarioMode;
    private String scenarioLabel;
    private DemoRecommendationViewVO tagOverlapView;
    private DemoRecommendationViewVO fullPipelineView;
}
