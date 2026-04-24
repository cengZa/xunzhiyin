package com.lcj.campusreco.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.lcj.campusreco.config.RecommendationTuningContext;
import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.RuleHitModel;
import com.lcj.campusreco.domain.model.TrustScoreResult;
import com.lcj.campusreco.service.TrustScoreService;
import com.lcj.campusreco.service.UserService;
import com.lcj.campusreco.strategy.rerank.RerankRule;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RerankServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private RerankRule rerankRule;

    @Mock
    private TrustScoreService trustScoreService;

    @Test
    void rerankAppliesWeightScaleOverrideAndTrustScore() {
        RecommendationTuningContext tuningContext =
                new RecommendationTuningContext(5, BigDecimal.ONE, "study_partner", true);
        RerankServiceImpl service = new RerankServiceImpl(
                userService,
                List.of(rerankRule),
                tuningContext,
                trustScoreService,
                new BigDecimal("0.15")
        );

        UserEntity requestUser = new UserEntity();
        requestUser.setId(1L);
        UserEntity targetUser = new UserEntity();
        targetUser.setId(2L);

        RankingCandidateModel candidate = new RankingCandidateModel();
        candidate.setTargetUserId(2L);
        candidate.setRankScore(new BigDecimal("0.8000"));

        RuleHitModel hit = new RuleHitModel();
        hit.setRuleCode("MAJOR_RELATED");
        hit.setRuleDesc("专业方向相近");
        hit.setHit(Boolean.TRUE);
        hit.setAdjustScore(new BigDecimal("0.1000"));

        when(userService.getById(1L)).thenReturn(requestUser);
        when(userService.getById(2L)).thenReturn(targetUser);
        when(rerankRule.apply(requestUser, targetUser, candidate)).thenReturn(hit);
        when(trustScoreService.evaluate(targetUser)).thenReturn(
                new TrustScoreResult(new BigDecimal("0.4000"), List.of("资料完整"))
        );

        try (RecommendationTuningContext.Scope ignored = tuningContext.withOverrides(null, new BigDecimal("1.5"), "study_partner", true)) {
            List<RankingCandidateModel> result = service.rerank(1L, List.of(candidate));
            assertEquals("study_partner", result.getFirst().getScenarioMode());
            assertEquals("学习搭子", result.getFirst().getScenarioLabel());
            assertEquals(new BigDecimal("0.2100"), result.getFirst().getRerankScore());
            assertEquals(new BigDecimal("0.4000"), result.getFirst().getTrustScore());
            assertEquals(new BigDecimal("1.0700"), result.getFirst().getFinalScore());
        }
    }
}
