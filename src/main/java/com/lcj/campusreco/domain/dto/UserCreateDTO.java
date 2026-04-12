package com.lcj.campusreco.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserCreateDTO {

    private String studentNo;

    @NotBlank
    private String nickname;

    private Integer gender;

    @NotNull
    private Integer grade;

    @NotBlank
    private String major;

    @NotBlank
    private String college;

    private String bio;
}
