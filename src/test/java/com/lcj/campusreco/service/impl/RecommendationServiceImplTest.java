package com.lcj.campusreco.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lcj.campusreco.domain.dto.RecommendRequestDTO;
import com.lcj.campusreco.domain.entity.RecommendationResultEntity;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.UserProfileModel;
import com.lcj.campusreco.domain.vo.ExplanationVO;
import com.lcj.campusreco.infra.repository.RecommendationQueryRepository;
import com.lcj.campusreco.mapper.RecommendationResultMapper;
import com.lcj.campusreco.service.ExplanationService;
import com.lcj.campusreco.service.ProfileService;
import com.lcj.campusreco.service.RankingService;
import com.lcj.campusreco.service.RecallService;
import com.lcj.campusreco.service.RerankService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @Mock
    private ProfileService profileService;
    @Mock
    private RecallService recallService;
    @Mock
    private RankingService rankingService;
    @Mock
    private RerankService rerankService;
    @Mock
    private ExplanationService explanationService;
    @Mock
    private RecommendationQueryRepository recommendationQueryRepository;
    @Mock
    private RecommendationResultMapper recommendationResultMapper;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    @Test
    void recommendReturnsTopKItemsAndPersistsResults() {
        RecommendRequestDTO requestDTO = new RecommendRequestDTO();
        requestDTO.setUserId(1L);
        requestDTO.setTopK(1);
        requestDTO.setUseCache(Boolean.FALSE);

        UserProfileModel profileModel = new UserProfileModel();
        profileModel.setUserId(1L);
        profileModel.getVector().put(101L, BigDecimal.ONE);

        RankingCandidateModel candidate = new RankingCandidateModel();
        candidate.setTargetUserId(2L);
        candidate.setRecallScore(BigDecimal.ONE);
        candidate.setRankScore(BigDecimal.valueOf(0.9D));
        candidate.setRerankScore(BigDecimal.valueOf(0.1D));
        candidate.setFinalScore(BigDecimal.ONE);

        ExplanationVO explanationVO = new ExplanationVO();
        explanationVO.setReasonText("Recommended because you both share music");

        when(profileService.getProfile(1L)).thenReturn(profileModel);
        when(recallService.recallCandidateUserIds(profileModel)).thenReturn(Set.of(2L));
        when(rankingService.rank(1L, Set.of(2L))).thenReturn(List.of(candidate));
        when(rerankService.rerank(1L, List.of(candidate))).thenReturn(List.of(candidate));
        when(explanationService.generate(candidate)).thenReturn(explanationVO);

        AtomicLong idSequence = new AtomicLong(1000L);
        doAnswer(invocation -> {
            RecommendationResultEntity entity = invocation.getArgument(0);
            entity.setId(idSequence.incrementAndGet());
            return 1;
        }).when(recommendationResultMapper).insert(any(RecommendationResultEntity.class));

        var result = recommendationService.recommend(requestDTO);

        assertEquals(1, result.getItems().size());
        assertEquals(1, result.getRecallCandidatesCount());
        assertFalse(result.getRequestTraceId().isBlank());
        assertEquals(2L, result.getItems().getFirst().getTargetUserId());
        assertEquals("Recommended because you both share music", result.getItems().getFirst().getExplanation());
        verify(recommendationResultMapper).insert(any(RecommendationResultEntity.class));
        verify(explanationService).batchSaveExplanation(any(), any());
    }
}
