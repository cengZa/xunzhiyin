package com.lcj.campusreco.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user")
public class UserEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String studentNo;
    private String nickname;
    private Integer gender;
    private Integer grade;
    private String major;
    private String college;
    private String bio;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
