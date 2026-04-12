package com.lcj.campusreco.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_feedback")
public class UserFeedbackEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long requestUserId;
    private Long targetUserId;
    private Long recommendationId;
    private String feedbackType;
    private LocalDateTime feedbackTime;
    private String feedbackNote;
    private LocalDateTime createdAt;
}
