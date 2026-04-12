package com.lcj.campusreco.service;

import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.vo.ExplanationVO;
import java.util.List;
import java.util.Map;

public interface ExplanationService {

    ExplanationVO generate(RankingCandidateModel candidate);

    void batchSaveExplanation(List<RankingCandidateModel> candidates, Map<Long, Long> recommendationIdMap);

    ExplanationVO getByRecommendationId(Long recommendationId);
}
