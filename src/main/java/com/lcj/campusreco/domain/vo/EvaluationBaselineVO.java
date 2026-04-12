package com.lcj.campusreco.domain.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class EvaluationBaselineVO {

    private String baselineCode;
    private String baselineName;
    private Integer evaluatedUserCount;
    private BigDecimal averageRecallCandidateCount;
    private BigDecimal averageTopKReturnCount;
    private BigDecimal precisionAtK;
    private BigDecimal hitRateAtK;
    private BigDecimal explanationPresenceRate;
}
