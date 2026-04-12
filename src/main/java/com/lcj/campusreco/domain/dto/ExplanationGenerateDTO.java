package com.lcj.campusreco.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExplanationGenerateDTO {

    @NotNull
    private Long recommendationId;

    @NotNull
    private Long requestUserId;

    @NotNull
    private Long targetUserId;
}
