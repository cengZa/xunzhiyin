package com.lcj.campusreco.strategy.rerank;

import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.RuleHitModel;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class MajorRelatedRule implements RerankRule {

    @Override
    public RuleHitModel apply(UserEntity requestUser, UserEntity candidateUser, RankingCandidateModel candidateModel) {
        RuleHitModel ruleHitModel = new RuleHitModel();
        ruleHitModel.setRuleCode("MAJOR_RELATED");
        ruleHitModel.setRuleDesc("专业相同或相近加分");
        boolean hit = requestUser != null
                && candidateUser != null
                && requestUser.getMajor() != null
                && requestUser.getMajor().equalsIgnoreCase(candidateUser.getMajor());
        ruleHitModel.setHit(hit);
        ruleHitModel.setAdjustScore(hit ? BigDecimal.valueOf(0.08D) : BigDecimal.ZERO);
        return ruleHitModel;
    }
}
