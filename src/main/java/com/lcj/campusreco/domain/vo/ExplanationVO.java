package com.lcj.campusreco.domain.vo;

import lombok.Data;

@Data
public class ExplanationVO {

    private Long recommendationId;
    private String reasonText;
    private Object evidenceJson;
    private Object contributionJson;
    private Object evidence;
    private Object contribution;
}
