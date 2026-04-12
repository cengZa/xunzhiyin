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
    private BigDecimal rerankScore;
    private BigDecimal finalScore;
    private List<ContributionItemModel> contributions = new ArrayList<>();
    private List<RuleHitModel> ruleHits = new ArrayList<>();
}
