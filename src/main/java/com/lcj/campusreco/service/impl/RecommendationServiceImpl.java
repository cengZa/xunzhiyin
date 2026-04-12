package com.lcj.campusreco.service.impl;

import com.lcj.campusreco.domain.entity.RecommendationResultEntity;
import com.lcj.campusreco.domain.dto.RecommendRequestDTO;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.UserProfileModel;
import com.lcj.campusreco.domain.vo.RecommendationDetailVO;
import com.lcj.campusreco.domain.vo.RecommendationItemVO;
import com.lcj.campusreco.infra.repository.RecommendationQueryRepository;
import com.lcj.campusreco.mapper.RecommendationResultMapper;
import com.lcj.campusreco.service.ExplanationService;
import com.lcj.campusreco.service.ProfileService;
import com.lcj.campusreco.service.RankingService;
import com.lcj.campusreco.service.RecallService;
import com.lcj.campusreco.service.RecommendationService;
import com.lcj.campusreco.service.RerankService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final ProfileService profileService;
    private final RecallService recallService;
    private final RankingService rankingService;
    private final RerankService rerankService;
    private final ExplanationService explanationService;
    private final RecommendationQueryRepository recommendationQueryRepository;
    private final RecommendationResultMapper recommendationResultMapper;

    public RecommendationServiceImpl(ProfileService profileService,
                                     RecallService recallService,
                                     RankingService rankingService,
                                     RerankService rerankService,
                                     ExplanationService explanationService,
                                     RecommendationQueryRepository recommendationQueryRepository,
                                     RecommendationResultMapper recommendationResultMapper) {
        this.profileService = profileService;
        this.recallService = recallService;
        this.rankingService = rankingService;
        this.rerankService = rerankService;
        this.explanationService = explanationService;
        this.recommendationQueryRepository = recommendationQueryRepository;
        this.recommendationResultMapper = recommendationResultMapper;
    }

    @Override
    public RecommendationDetailVO recommend(RecommendRequestDTO dto) {
        UserProfileModel profileModel = profileService.getProfile(dto.getUserId());
        if (profileModel.getVector().isEmpty()) {
            profileModel = profileService.buildProfile(dto.getUserId(), "init");
        }
        var candidateUserIds = recallService.recallCandidateUserIds(profileModel);
        List<RankingCandidateModel> rankingList = rankingService.rank(dto.getUserId(), candidateUserIds);
        List<RankingCandidateModel> rerankedList = rerankService.rerank(dto.getUserId(), rankingList);
        List<RankingCandidateModel> topList = rerankedList.stream().limit(dto.getTopK()).toList();

        RecommendationDetailVO detailVO = new RecommendationDetailVO();
        String traceId = UUID.randomUUID().toString();
        detailVO.setRequestTraceId(traceId);
        detailVO.setRecallCandidatesCount(candidateUserIds.size());
        detailVO.setRecallCandidateCount(candidateUserIds.size());
        Map<Long, Long> recommendationIdMap = saveRecommendationResults(dto.getUserId(), traceId, topList);
        explanationService.batchSaveExplanation(topList, recommendationIdMap);

        int rankNo = 1;
        for (RankingCandidateModel candidate : topList) {
            RecommendationItemVO itemVO = new RecommendationItemVO();
            itemVO.setRecommendationId(recommendationIdMap.get(candidate.getTargetUserId()));
            itemVO.setTargetUserId(candidate.getTargetUserId());
            itemVO.setFinalScore(candidate.getFinalScore());
            itemVO.setRankNo(rankNo++);
            String explanation = explanationService.generate(candidate).getReasonText();
            itemVO.setExplanation(explanation);
            itemVO.setReasonText(explanation);
            detailVO.getItems().add(itemVO);
        }
        return detailVO;
    }

    @Override
    public RecommendationDetailVO getRecommendationDetail(Long userId) {
        RecommendationDetailVO detailVO = new RecommendationDetailVO();
        int size = recommendationQueryRepository.listByRequestUserId(userId).size();
        detailVO.setRecallCandidatesCount(size);
        detailVO.setRecallCandidateCount(size);
        return detailVO;
    }

    private Map<Long, Long> saveRecommendationResults(Long requestUserId,
                                                      String traceId,
                                                      List<RankingCandidateModel> topList) {
        Map<Long, Long> recommendationIdMap = new LinkedHashMap<>();
        int rankNo = 1;
        for (RankingCandidateModel candidate : topList) {
            RecommendationResultEntity entity = new RecommendationResultEntity();
            entity.setRequestUserId(requestUserId);
            entity.setTargetUserId(candidate.getTargetUserId());
            entity.setRecallScore(candidate.getRecallScore());
            entity.setRankScore(candidate.getRankScore());
            entity.setRerankScore(candidate.getRerankScore());
            entity.setFinalScore(candidate.getFinalScore());
            entity.setRankNo(rankNo++);
            entity.setRequestTraceId(traceId);
            entity.setCreatedAt(LocalDateTime.now());
            recommendationResultMapper.insert(entity);
            recommendationIdMap.put(candidate.getTargetUserId(), entity.getId());
        }
        return recommendationIdMap;
    }
}
