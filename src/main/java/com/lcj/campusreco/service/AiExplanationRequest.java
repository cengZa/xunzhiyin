package com.lcj.campusreco.service;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AiExplanationRequest {

    Long recommendationId;
    String scenarioMode;
    String ruleReasonText;
    Object evidence;
    Object contribution;
    String evidenceJson;
    String contributionJson;
}
