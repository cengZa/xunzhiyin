package com.lcj.campusreco.domain.model;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class TagWeightModel {

    private Long tagId;
    private String tagName;
    private String tagType;
    private BigDecimal tf;
    private BigDecimal idf;
    private BigDecimal timeDecay;
    private BigDecimal weightSeed;
    private BigDecimal finalWeight;
}
