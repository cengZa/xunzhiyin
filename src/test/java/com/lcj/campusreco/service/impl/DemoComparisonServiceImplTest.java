package com.lcj.campusreco.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.lcj.campusreco.config.RecommendationTuningContext;
import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.model.ContributionItemModel;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.RuleHitModel;
import com.lcj.campusreco.domain.model.UserProfileModel;
import com.lcj.campusreco.domain.vo.DemoComparisonVO;
import com.lcj.campusreco.domain.vo.ExplanationVO;
import com.lcj.campusreco.service.ExplorationService;
import com.lcj.campusreco.service.ExplanationService;
import com.lcj.campusreco.service.ProfileService;
import com.lcj.campusreco.service.RankingService;
import com.lcj.campusreco.service.RecallService;
import com.lcj.campusreco.service.RerankService;
import com.lcj.campusreco.service.UserService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DemoComparisonServiceImplTest {

    @Mock
    private ProfileService profileService;
    @Mock
    private RecallService recallService;
    @Mock
    private RankingService rankingService;
    @Mock
    private RerankService rerankService;
    @Mock
    private ExplorationService explorationService;
    @Mock
    private ExplanationService explanationService;
    @Mock
    private UserService userService;

    @Test
    void compareViewsReturnsScenarioAwareOrders() {
        DemoComparisonServiceImpl demoComparisonService = new DemoComparisonServiceImpl(
                profileService,
                recallService,
                rankingService,
                rerankService,
                explorationService,
                explanationService,
                userService,
                new RecommendationTuningContext(5, BigDecimal.ONE, "interest_partner", true)
        );

        UserProfileModel profile = new UserProfileModel();
        profile.setUserId(2001L);
        profile.getVector().put(101L, BigDecimal.ONE);

        RankingCandidateModel first = createCandidate(2002L, "3.0", "0.7", "0.0", "0.7", "Java");
        RankingCandidateModel second = createCandidate(2005L, "2.0", "0.9", "0.1", "1.0", "AI");
        second.setScenarioMode("study_partner");
        second.setScenarioLabel("学习搭子");
        second.setCampusScore(new BigDecimal("0.1000"));
        second.setTrustScore(new BigDecimal("0.2700"));
        second.getTrustReasons().add("资料完整");

        when(profileService.getProfile(2001L)).thenReturn(profile);
        when(recallService.recallCandidateUserIds(profile)).thenReturn(Set.of(2002L, 2005L));
        when(rankingService.rank(2001L, Set.of(2002L, 2005L))).thenReturn(List.of(first, second));
        when(rerankService.rerank(eq(2001L), any())).thenReturn(List.of(second, first));
        when(explorationService.apply(eq(2001L), any(), eq(2), eq("study_partner"))).thenReturn(List.of(second, first));
        when(userService.getById(2002L)).thenReturn(createUser(2002L, "柏宇"));
        when(userService.getById(2005L)).thenReturn(createUser(2005L, "顾宁"));

        ExplanationVO explanation = new ExplanationVO();
        explanation.setReasonText("推荐原因：你们都对 Java 更感兴趣");
        when(explanationService.generate(any())).thenReturn(explanation);

        DemoComparisonVO comparison = demoComparisonService.compareViews(2001L, 2, "study_partner");

        assertEquals(2001L, comparison.getUserId());
        assertEquals("study_partner", comparison.getScenarioMode());
        assertEquals("学习搭子", comparison.getScenarioLabel());
        assertEquals("tag_overlap", comparison.getTagOverlapView().getViewCode());
        assertEquals("full_pipeline", comparison.getFullPipelineView().getViewCode());
        assertEquals(2002L, comparison.getTagOverlapView().getItems().getFirst().getTargetUserId());
        assertEquals(2005L, comparison.getFullPipelineView().getItems().getFirst().getTargetUserId());
        assertEquals("学习搭子", comparison.getFullPipelineView().getItems().getFirst().getScenarioLabel());
        assertTrue(comparison.getFullPipelineView().getSummary().contains("可信连接分"));
    }

    private RankingCandidateModel createCandidate(Long userId,
                                                  String recallScore,
                                                  String rankScore,
                                                  String rerankScore,
                                                  String finalScore,
                                                  String tagName) {
        RankingCandidateModel candidate = new RankingCandidateModel();
        candidate.setTargetUserId(userId);
        candidate.setRecallScore(new BigDecimal(recallScore));
        candidate.setRankScore(new BigDecimal(rankScore));
        candidate.setInterestScore(new BigDecimal(rankScore));
        candidate.setRerankScore(new BigDecimal(rerankScore));
        candidate.setCampusScore(new BigDecimal(rerankScore));
        candidate.setFinalScore(new BigDecimal(finalScore));

        ContributionItemModel contribution = new ContributionItemModel();
        contribution.setTagId(101L);
        contribution.setTagName(tagName);
        contribution.setContributionScore(new BigDecimal("0.8"));
        candidate.getContributions().add(contribution);

        RuleHitModel ruleHit = new RuleHitModel();
        ruleHit.setRuleCode("MAJOR_RELATED");
        ruleHit.setRuleDesc("专业方向相近");
        ruleHit.setHit(Boolean.TRUE);
        ruleHit.setAdjustScore(new BigDecimal("0.1"));
        candidate.getRuleHits().add(ruleHit);
        return candidate;
    }

    private UserEntity createUser(Long userId, String nickname) {
        UserEntity entity = new UserEntity();
        entity.setId(userId);
        entity.setNickname(nickname);
        return entity;
    }
}
