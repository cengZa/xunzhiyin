package com.lcj.campusreco.domain.vo;

import java.util.List;
import lombok.Data;

@Data
public class UserVO {

    private Long userId;
    private String studentNo;
    private String nickname;
    private Integer gender;
    private Integer grade;
    private String major;
    private String college;
    private String bio;
    private List<String> tags;
}
