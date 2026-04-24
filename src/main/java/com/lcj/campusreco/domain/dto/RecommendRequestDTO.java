package com.lcj.campusreco.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecommendRequestDTO {

    @NotNull
    private Long userId;

    @Min(1)
    private Integer topK = 10;

    private Boolean useCache = Boolean.TRUE;

    private String scenarioMode = "interest_partner";
}
