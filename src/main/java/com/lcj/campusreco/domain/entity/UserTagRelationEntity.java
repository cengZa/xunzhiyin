package com.lcj.campusreco.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_tag_relation")
public class UserTagRelationEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long tagId;
    private String sourceType;
    private LocalDateTime selectedAt;
    private BigDecimal weightSeed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
