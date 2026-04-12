package com.lcj.campusreco.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BuildProfileDTO {

    @NotNull
    private Long userId;

    @NotBlank
    private String updatedBy;
}
