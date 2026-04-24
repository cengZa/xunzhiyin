package com.lcj.campusreco.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExplanationVO {

    private String recommendationId;
    private String reasonText;
    private String ruleReasonText;
    private String llmReasonText;
    private String reasonSource;
    private Object evidenceJson;
    private Object contributionJson;
    private Object evidence;
    private Object contribution;
}
