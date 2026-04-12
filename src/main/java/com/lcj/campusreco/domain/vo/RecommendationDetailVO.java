package com.lcj.campusreco.domain.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class RecommendationDetailVO {

    private String requestTraceId;
    private Integer recallCandidatesCount;
    private Integer recallCandidateCount;
    private List<RecommendationItemVO> items = new ArrayList<>();
    private Object rankingDetails;
    private Object rerankRuleHits;
    private Object explanationEvidence;
}
