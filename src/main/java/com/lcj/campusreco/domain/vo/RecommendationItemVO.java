package com.lcj.campusreco.domain.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class RecommendationItemVO {

    private Long recommendationId;
    private Long targetUserId;
    private String targetNickname;
    private BigDecimal finalScore;
    private Integer rankNo;
    private String explanation;
    private String reasonText;
}
