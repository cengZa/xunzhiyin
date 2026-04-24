package com.lcj.campusreco.service;

import com.lcj.campusreco.domain.dto.RecommendRequestDTO;
import com.lcj.campusreco.domain.vo.RecommendationDetailVO;

public interface RecommendationService {

    RecommendationDetailVO recommend(RecommendRequestDTO dto);

    RecommendationDetailVO getRecommendationDetail(Long userId, String scenarioMode);
}
