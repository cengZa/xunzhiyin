package com.lcj.campusreco.domain.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class RecommendationItemVO {

    private String recommendationId;
    private Long targetUserId;
    private String targetNickname;
    private BigDecimal recallScore;
    private BigDecimal rankScore;
    private BigDecimal interestScore;
    private BigDecimal rerankScore;
    private BigDecimal campusScore;
    private BigDecimal trustScore;
    private BigDecimal explorationScore;
    private BigDecimal finalScore;
    private Integer rankNo;
    private String scenarioMode;
    private String scenarioLabel;
    private boolean exploration;
    private String explorationReason;
    private List<String> matchedTags = new ArrayList<>();
    private List<String> matchedRules = new ArrayList<>();
    private List<String> trustReasons = new ArrayList<>();
    private String recommendationLabel;
    private String explanation;
    private String reasonText;
}
