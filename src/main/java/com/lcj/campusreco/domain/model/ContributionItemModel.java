package com.lcj.campusreco.domain.model;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class ContributionItemModel {

    private Long tagId;
    private String tagName;
    private BigDecimal sourceWeight;
    private BigDecimal targetWeight;
    private BigDecimal contributionScore;
}
