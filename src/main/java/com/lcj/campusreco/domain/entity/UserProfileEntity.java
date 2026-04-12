package com.lcj.campusreco.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_profile")
public class UserProfileEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Integer profileVersion;
    private String profileJson;
    private String topkJson;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
