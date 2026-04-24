package com.lcj.campusreco.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lcj.campusreco.config.RecommendationTuningContext;
import com.lcj.campusreco.domain.dto.RecommendRequestDTO;
import com.lcj.campusreco.domain.entity.RecommendationResultEntity;
import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.model.ContributionItemModel;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.RuleHitModel;
import com.lcj.campusreco.domain.model.UserProfileModel;
import com.lcj.campusreco.domain.vo.ExplanationVO;
import com.lcj.campusreco.infra.repository.RecommendationQueryRepository;
import com.lcj.campusreco.mapper.RecommendationResultMapper;
import com.lcj.campusreco.service.ExplorationService;
import com.lcj.campusreco.service.ExplanationService;
import com.lcj.campusreco.service.ProfileService;
import com.lcj.campusreco.service.RankingService;
import com.lcj.campusreco.service.RecallService;
import com.lcj.campusreco.service.RerankService;
import com.lcj.campusreco.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private ExplorationService explorationService;
    @Mock
    private UserService userService;
    @Mock
    private RecommendationQueryRepository recommendationQueryRepository;
    @Mock
    private RecommendationResultMapper recommendationResultMapper;

    private RecommendationServiceImpl recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationServiceImpl(
                profileService,
                recallService,
                rankingService,
                rerankService,
                explorationService,
                explanationService,
                userService,
                recommendationQueryRepository,
                recommendationResultMapper,
                new RecommendationTuningContext(5, new BigDecimal("1.0"), "interest_partner", true)
        );
    }

    @Test
    void recommendReturnsScenarioAndTrustBreakdown() {
        RecommendRequestDTO requestDTO = new RecommendRequestDTO();
        requestDTO.setUserId(1L);
        requestDTO.setTopK(1);
        requestDTO.setUseCache(Boolean.FALSE);
        requestDTO.setScenarioMode("study_partner");

        UserProfileModel profileModel = new UserProfileModel();
        profileModel.setUserId(1L);
        profileModel.getVector().put(101L, BigDecimal.ONE);

        RankingCandidateModel candidate = new RankingCandidateModel();
        candidate.setTargetUserId(2L);
        candidate.setRecallScore(BigDecimal.ONE);
        candidate.setRankScore(new BigDecimal("0.9000"));
        candidate.setInterestScore(new BigDecimal("0.9000"));
        candidate.setRerankScore(new BigDecimal("0.1200"));
        candidate.setCampusScore(new BigDecimal("0.1200"));
        candidate.setTrustScore(new BigDecimal("0.2700"));
        candidate.setTrustReasons(List.of("资料完整", "标签丰富", "历史关注较多"));
        candidate.setFinalScore(new BigDecimal("1.0605"));
        candidate.setScenarioMode("study_partner");
        candidate.setScenarioLabel("学习搭子");
        candidate.getContributions().add(createContribution("AI", "0.56"));
        candidate.getContributions().add(createContribution("Music", "0.32"));
        candidate.getRuleHits().add(createRuleHit("MAJOR_RELATED", "专业方向相近", true, "0.08"));
        candidate.getRuleHits().add(createRuleHit("CLUB_OVERLAP", "校园社团兴趣重合", true, "0.04"));

        ExplanationVO explanationVO = new ExplanationVO();
        explanationVO.setReasonText("推荐原因：你们在 AI 和音乐上兴趣高度重合，同时命中了校园社团场景加权。");
        explanationVO.setEvidence(Map.of("sharedTags", List.of("AI", "Music"), "ruleHits", candidate.getRuleHits()));
        explanationVO.setContribution(candidate.getContributions());

        when(profileService.getProfile(1L)).thenReturn(profileModel);
        when(recallService.recallCandidateUserIds(profileModel)).thenReturn(Set.of(2L));
        when(rankingService.rank(1L, Set.of(2L))).thenReturn(List.of(candidate));
        when(rerankService.rerank(1L, List.of(candidate))).thenReturn(List.of(candidate));
        when(explorationService.apply(1L, List.of(candidate), 1, "study_partner")).thenReturn(List.of(candidate));
        when(explanationService.generate(candidate)).thenReturn(explanationVO);
        when(userService.getById(2L)).thenReturn(createUser(2L, "小林"));

        AtomicLong idSequence = new AtomicLong(1000L);
        doAnswer(invocation -> {
            RecommendationResultEntity entity = invocation.getArgument(0);
            entity.setId(idSequence.incrementAndGet());
            return 1;
        }).when(recommendationResultMapper).insert(any(RecommendationResultEntity.class));

        var result = recommendationService.recommend(requestDTO);

        assertEquals(1, result.getItems().size());
        assertEquals("study_partner", result.getScenarioMode());
        assertEquals("学习搭子", result.getScenarioLabel());
        assertEquals(2L, result.getItems().getFirst().getTargetUserId());
        assertEquals("小林", result.getItems().getFirst().getTargetNickname());
        assertEquals(new BigDecimal("0.9000"), result.getItems().getFirst().getInterestScore());
        assertEquals(new BigDecimal("0.1200"), result.getItems().getFirst().getCampusScore());
        assertEquals(new BigDecimal("0.2700"), result.getItems().getFirst().getTrustScore());
        assertEquals(List.of("资料完整", "标签丰富", "历史关注较多"), result.getItems().getFirst().getTrustReasons());
        assertEquals("study_partner", result.getItems().getFirst().getScenarioMode());
        assertEquals("学习搭子", result.getItems().getFirst().getScenarioLabel());
        assertEquals(List.of("AI", "Music"), result.getItems().getFirst().getMatchedTags());
        assertEquals(List.of("专业方向相近", "校园社团兴趣重合"), result.getItems().getFirst().getMatchedRules());
        assertFalse(((List<?>) result.getExplanationEvidence()).isEmpty());
        verify(recommendationResultMapper).insert(any(RecommendationResultEntity.class));
        verify(explanationService).batchSaveExplanation(any(), any());
    }

    @Test
    void recommendCarriesExplorationMetadata() {
        RecommendRequestDTO requestDTO = new RecommendRequestDTO();
        requestDTO.setUserId(1L);
        requestDTO.setTopK(3);
        requestDTO.setUseCache(Boolean.FALSE);
        requestDTO.setScenarioMode("interest_partner");

        UserProfileModel profileModel = new UserProfileModel();
        profileModel.setUserId(1L);
        profileModel.getVector().put(101L, BigDecimal.ONE);

        RankingCandidateModel first = createCandidateModel(2L, "0.9200", "0.1800", "0.2500", "1.1375", "interest_partner");
        RankingCandidateModel second = createCandidateModel(3L, "0.8800", "0.1200", "0.2100", "1.0315", "interest_partner");
        RankingCandidateModel exploration = createCandidateModel(4L, "0.8100", "0.0400", "0.2200", "0.8830", "interest_partner");
        exploration.setExploration(true);
        exploration.setExplorationScore(new BigDecimal("0.7340"));
        exploration.setExplorationReason("跨专业但兴趣标签高度重合，适合作为轻量探索位观察潜在连接");

        List<RankingCandidateModel> ranked = List.of(first, second, exploration);

        when(profileService.getProfile(1L)).thenReturn(profileModel);
        when(recallService.recallCandidateUserIds(profileModel)).thenReturn(Set.of(2L, 3L, 4L));
        when(rankingService.rank(1L, Set.of(2L, 3L, 4L))).thenReturn(ranked);
        when(rerankService.rerank(1L, ranked)).thenReturn(ranked);
        when(explorationService.apply(1L, ranked, 3, "interest_partner")).thenReturn(ranked);
        when(explanationService.generate(any())).thenAnswer(invocation -> {
            RankingCandidateModel candidate = invocation.getArgument(0);
            ExplanationVO explanationVO = new ExplanationVO();
            explanationVO.setReasonText("测试解释");
            explanationVO.setEvidence(Map.of(
                    "exploration", candidate.isExploration(),
                    "explorationScore", candidate.getExplorationScore(),
                    "explorationReason", candidate.getExplorationReason()
            ));
            explanationVO.setContribution(candidate.getContributions());
            return explanationVO;
        });
        when(userService.getById(2L)).thenReturn(createUser(2L, "小林"));
        when(userService.getById(3L)).thenReturn(createUser(3L, "小周"));
        when(userService.getById(4L)).thenReturn(createUser(4L, "小陈"));

        AtomicLong idSequence = new AtomicLong(2000L);
        doAnswer(invocation -> {
            RecommendationResultEntity entity = invocation.getArgument(0);
            entity.setId(idSequence.incrementAndGet());
            return 1;
        }).when(recommendationResultMapper).insert(any(RecommendationResultEntity.class));

        var result = recommendationService.recommend(requestDTO);

        assertEquals(3, result.getItems().size());
        assertEquals(4L, result.getItems().get(2).getTargetUserId());
        assertEquals(new BigDecimal("0.7340"), result.getItems().get(2).getExplorationScore());
        assertEquals("跨专业但兴趣标签高度重合，适合作为轻量探索位观察潜在连接", result.getItems().get(2).getExplorationReason());
        assertEquals(true, result.getItems().get(2).isExploration());
    }

    private RankingCandidateModel createCandidateModel(Long userId,
                                                       String rankScore,
                                                       String campusScore,
                                                       String trustScore,
                                                       String finalScore,
                                                       String scenarioMode) {
        RankingCandidateModel candidate = new RankingCandidateModel();
        candidate.setTargetUserId(userId);
        candidate.setRecallScore(BigDecimal.ONE);
        candidate.setRankScore(new BigDecimal(rankScore));
        candidate.setInterestScore(new BigDecimal(rankScore));
        candidate.setRerankScore(new BigDecimal(campusScore));
        candidate.setCampusScore(new BigDecimal(campusScore));
        candidate.setTrustScore(new BigDecimal(trustScore));
        candidate.setFinalScore(new BigDecimal(finalScore));
        candidate.setScenarioMode(scenarioMode);
        candidate.setScenarioLabel("兴趣搭子");
        candidate.getContributions().add(createContribution("AI", "0.56"));
        return candidate;
    }

    private ContributionItemModel createContribution(String tagName, String contributionScore) {
        ContributionItemModel item = new ContributionItemModel();
        item.setTagName(tagName);
        item.setContributionScore(new BigDecimal(contributionScore));
        return item;
    }

    private RuleHitModel createRuleHit(String ruleCode, String ruleDesc, boolean hit, String adjustScore) {
        RuleHitModel ruleHit = new RuleHitModel();
        ruleHit.setRuleCode(ruleCode);
        ruleHit.setRuleDesc(ruleDesc);
        ruleHit.setHit(hit);
        ruleHit.setAdjustScore(new BigDecimal(adjustScore));
        return ruleHit;
    }

    private UserEntity createUser(Long userId, String nickname) {
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setNickname(nickname);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
