package com.lcj.campusreco.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class UserTagBindDTO {

    @NotEmpty
    private List<Long> tagIds;

    @NotBlank
    private String sourceType;
}
