package com.lcj.campusreco.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeedbackSubmitDTO {

    @NotNull
    private Long recommendationId;

    @NotNull
    private Long targetUserId;

    @NotBlank
    private String feedbackType;
}
