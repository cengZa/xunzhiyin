package com.lcj.campusreco.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("recommendation_result")
public class RecommendationResultEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long requestUserId;
    private Long targetUserId;
    private BigDecimal recallScore;
    private BigDecimal rankScore;
    private BigDecimal rerankScore;
    private BigDecimal finalScore;
    private Integer rankNo;
    private String requestTraceId;
    private LocalDateTime createdAt;
}
