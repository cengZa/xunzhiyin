package com.lcj.campusreco.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class RankingCandidateModel {

    private Long targetUserId;
    private BigDecimal recallScore;
    private BigDecimal rankScore;
    private BigDecimal interestScore;
    private BigDecimal rerankScore;
    private BigDecimal campusScore;
    private BigDecimal trustScore;
    private BigDecimal explorationScore = BigDecimal.ZERO;
    private BigDecimal finalScore;
    private String scenarioMode;
    private String scenarioLabel;
    private boolean exploration;
    private String explorationReason = "";
    private List<String> trustReasons = new ArrayList<>();
    private List<ContributionItemModel> contributions = new ArrayList<>();
    private List<RuleHitModel> ruleHits = new ArrayList<>();
}
