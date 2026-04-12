package com.lcj.campusreco.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("tag")
public class TagEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String tagName;
    private String tagType;
    private String tagDesc;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
