package com.lcj.campusreco.service.impl;

import com.lcj.campusreco.common.constant.RecommendationScenarioMode;
import com.lcj.campusreco.config.RecommendationTuningContext;
import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.RuleHitModel;
import com.lcj.campusreco.domain.model.TrustScoreResult;
import com.lcj.campusreco.service.RerankService;
import com.lcj.campusreco.service.TrustScoreService;
import com.lcj.campusreco.service.UserService;
import com.lcj.campusreco.strategy.rerank.RerankRule;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RerankServiceImpl implements RerankService {

    private final UserService userService;
    private final List<RerankRule> rerankRules;
    private final RecommendationTuningContext tuningContext;
    private final TrustScoreService trustScoreService;
    private final BigDecimal trustWeight;

    public RerankServiceImpl(UserService userService,
                             List<RerankRule> rerankRules,
                             RecommendationTuningContext tuningContext,
                             TrustScoreService trustScoreService,
                             @Value("${app.recommendation.trust-weight:0.15}") BigDecimal trustWeight) {
        this.userService = userService;
        this.rerankRules = rerankRules;
        this.tuningContext = tuningContext;
        this.trustScoreService = trustScoreService;
        this.trustWeight = trustWeight;
    }

    @Override
    public List<RankingCandidateModel> rerank(Long requestUserId, List<RankingCandidateModel> rankingList) {
        List<RankingCandidateModel> mutableRankingList = new ArrayList<>(rankingList);
        UserEntity requestUser = userService.getById(requestUserId);
        BigDecimal weightScale = tuningContext.getRerankWeightScale();
        String scenarioMode = tuningContext.getScenarioMode();
        boolean trustEnabled = tuningContext.isTrustEnabled();

        for (RankingCandidateModel candidateModel : mutableRankingList) {
            UserEntity candidateUser = userService.getById(candidateModel.getTargetUserId());
            List<RuleHitModel> ruleHits = new ArrayList<>();
            BigDecimal campusScore = BigDecimal.ZERO;
            for (RerankRule rerankRule : rerankRules) {
                RuleHitModel originalHit = rerankRule.apply(requestUser, candidateUser, candidateModel);
                RuleHitModel adjustedHit = adjustRuleForScenario(originalHit, scenarioMode);
                ruleHits.add(adjustedHit);
                if (adjustedHit.getAdjustScore() != null) {
                    campusScore = campusScore.add(adjustedHit.getAdjustScore().multiply(weightScale));
                }
            }

            BigDecimal interestScore = defaultScore(candidateModel.getRankScore());
            BigDecimal normalizedCampusScore = campusScore.setScale(4, RoundingMode.HALF_UP);
            BigDecimal trustScore = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            List<String> trustReasons = List.of();
            if (trustEnabled) {
                TrustScoreResult trustScoreResult = trustScoreService.evaluate(candidateUser);
                trustScore = trustScoreResult.score();
                trustReasons = trustScoreResult.reasons();
            }

            BigDecimal finalScore = interestScore
                    .add(normalizedCampusScore)
                    .add(trustScore.multiply(trustWeight))
                    .setScale(4, RoundingMode.HALF_UP);

            candidateModel.setScenarioMode(scenarioMode);
            candidateModel.setScenarioLabel(RecommendationScenarioMode.labelOf(scenarioMode));
            candidateModel.setInterestScore(interestScore);
            candidateModel.setRuleHits(ruleHits);
            candidateModel.setRerankScore(normalizedCampusScore);
            candidateModel.setCampusScore(normalizedCampusScore);
            candidateModel.setTrustScore(trustScore);
            candidateModel.setTrustReasons(new ArrayList<>(trustReasons));
            candidateModel.setFinalScore(finalScore);
        }
        mutableRankingList.sort(Comparator.comparing(
                RankingCandidateModel::getFinalScore,
                Comparator.nullsLast(BigDecimal::compareTo)
        ).reversed());
        return mutableRankingList;
    }

    private RuleHitModel adjustRuleForScenario(RuleHitModel originalHit, String scenarioMode) {
        RuleHitModel adjustedHit = new RuleHitModel();
        adjustedHit.setRuleCode(originalHit.getRuleCode());
        adjustedHit.setRuleDesc(originalHit.getRuleDesc());
        adjustedHit.setHit(originalHit.getHit());
        adjustedHit.setAdjustScore(defaultScore(originalHit.getAdjustScore())
                .multiply(resolveScenarioRuleMultiplier(scenarioMode, originalHit.getRuleCode()))
                .setScale(4, RoundingMode.HALF_UP));
        return adjustedHit;
    }

    private BigDecimal resolveScenarioRuleMultiplier(String scenarioMode, String ruleCode) {
        String normalizedMode = RecommendationScenarioMode.normalize(scenarioMode);
        return switch (normalizedMode) {
            case RecommendationScenarioMode.STUDY_PARTNER -> switch (ruleCode) {
                case "MAJOR_RELATED" -> new BigDecimal("1.4");
                case "GRADE_DIFF" -> new BigDecimal("1.2");
                case "CLUB_OVERLAP" -> new BigDecimal("0.4");
                default -> BigDecimal.ONE;
            };
            case RecommendationScenarioMode.CLUB_PARTNER -> switch (ruleCode) {
                case "CLUB_OVERLAP" -> new BigDecimal("1.8");
                case "GRADE_DIFF" -> BigDecimal.ONE;
                case "MAJOR_RELATED" -> new BigDecimal("0.5");
                default -> BigDecimal.ONE;
            };
            default -> switch (ruleCode) {
                case "CLUB_OVERLAP" -> BigDecimal.ONE;
                case "MAJOR_RELATED" -> new BigDecimal("0.7");
                case "GRADE_DIFF" -> new BigDecimal("0.6");
                default -> BigDecimal.ONE;
            };
        };
    }

    private BigDecimal defaultScore(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : value.setScale(4, RoundingMode.HALF_UP);
    }
}
