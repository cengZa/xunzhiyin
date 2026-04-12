package com.lcj.campusreco.service.impl;

import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.RuleHitModel;
import com.lcj.campusreco.service.RerankService;
import com.lcj.campusreco.service.UserService;
import com.lcj.campusreco.strategy.rerank.RerankRule;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RerankServiceImpl implements RerankService {

    private final UserService userService;
    private final List<RerankRule> rerankRules;

    public RerankServiceImpl(UserService userService, List<RerankRule> rerankRules) {
        this.userService = userService;
        this.rerankRules = rerankRules;
    }

    @Override
    public List<RankingCandidateModel> rerank(Long requestUserId, List<RankingCandidateModel> rankingList) {
        UserEntity requestUser = userService.getById(requestUserId);
        for (RankingCandidateModel candidateModel : rankingList) {
            UserEntity candidateUser = userService.getById(candidateModel.getTargetUserId());
            List<RuleHitModel> ruleHits = new ArrayList<>();
            BigDecimal rerankScore = BigDecimal.ZERO;
            for (RerankRule rerankRule : rerankRules) {
                RuleHitModel ruleHit = rerankRule.apply(requestUser, candidateUser, candidateModel);
                ruleHits.add(ruleHit);
                if (ruleHit.getAdjustScore() != null) {
                    rerankScore = rerankScore.add(ruleHit.getAdjustScore());
                }
            }
            candidateModel.setRuleHits(ruleHits);
            candidateModel.setRerankScore(rerankScore);
            candidateModel.setFinalScore((candidateModel.getRankScore() == null ? BigDecimal.ZERO : candidateModel.getRankScore()).add(rerankScore));
        }
        rankingList.sort(Comparator.comparing(RankingCandidateModel::getFinalScore, Comparator.nullsLast(BigDecimal::compareTo)).reversed());
        return rankingList;
    }
}
