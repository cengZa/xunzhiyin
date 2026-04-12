package com.lcj.campusreco.domain.model;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class RuleHitModel {

    private String ruleCode;
    private String ruleDesc;
    private Boolean hit;
    private BigDecimal adjustScore;
}
