package com.lcj.campusreco.domain.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class EvaluationSummaryVO {

    private String generatedAt;
    private Integer topK;
    private String scenarioMode;
    private String scenarioLabel;
    private Integer activeUserCount;
    private Integer tagCount;
    private Integer relationCount;
    private String proxyRelevanceRule;
    private List<EvaluationBaselineVO> baselines = new ArrayList<>();
}
