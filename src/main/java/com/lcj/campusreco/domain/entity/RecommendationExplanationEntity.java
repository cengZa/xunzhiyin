package com.lcj.campusreco.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("recommendation_explanation")
public class RecommendationExplanationEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long recommendationId;
    private String reasonText;
    private String evidenceJson;
    private String contributionJson;
    private LocalDateTime createdAt;
}
