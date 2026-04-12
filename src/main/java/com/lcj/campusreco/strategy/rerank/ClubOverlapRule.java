package com.lcj.campusreco.strategy.rerank;

import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.RuleHitModel;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class ClubOverlapRule implements RerankRule {

    @Override
    public RuleHitModel apply(UserEntity requestUser, UserEntity candidateUser, RankingCandidateModel candidateModel) {
        RuleHitModel ruleHitModel = new RuleHitModel();
        ruleHitModel.setRuleCode("CLUB_OVERLAP");
        ruleHitModel.setRuleDesc("社团重合度加分");
        ruleHitModel.setHit(Boolean.FALSE);
        ruleHitModel.setAdjustScore(BigDecimal.ZERO);
        return ruleHitModel;
    }
}
