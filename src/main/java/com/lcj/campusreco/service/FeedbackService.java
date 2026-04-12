package com.lcj.campusreco.service;

import com.lcj.campusreco.domain.dto.FeedbackSubmitDTO;
import com.lcj.campusreco.domain.entity.UserFeedbackEntity;
import java.util.List;

public interface FeedbackService {

    void submitFeedback(Long requestUserId, FeedbackSubmitDTO dto);

    void applyFeedbackUpdate(Long requestUserId, Long recommendationId, String feedbackType);

    List<UserFeedbackEntity> listByUserId(Long userId);
}
