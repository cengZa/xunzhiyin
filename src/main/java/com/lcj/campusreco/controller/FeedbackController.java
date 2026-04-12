package com.lcj.campusreco.controller;

import com.lcj.campusreco.common.api.ApiResponse;
import com.lcj.campusreco.domain.dto.FeedbackSubmitDTO;
import com.lcj.campusreco.domain.entity.UserFeedbackEntity;
import com.lcj.campusreco.service.FeedbackService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/api/recommendations/{userId}/feedback")
    public ApiResponse<Map<String, Boolean>> submitFeedback(@PathVariable Long userId, @Valid @RequestBody FeedbackSubmitDTO dto) {
        feedbackService.submitFeedback(userId, dto);
        return ApiResponse.success(Map.of("profileUpdated", Boolean.TRUE));
    }

    @GetMapping("/api/feedback/{userId}")
    public ApiResponse<List<UserFeedbackEntity>> listFeedback(@PathVariable Long userId) {
        return ApiResponse.success(feedbackService.listByUserId(userId));
    }
}
