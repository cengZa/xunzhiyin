package com.lcj.campusreco.strategy.rerank;

import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.RuleHitModel;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class GradeDiffRule implements RerankRule {

    @Override
    public RuleHitModel apply(UserEntity requestUser, UserEntity candidateUser, RankingCandidateModel candidateModel) {
        RuleHitModel ruleHitModel = new RuleHitModel();
        ruleHitModel.setRuleCode("GRADE_DIFF");
        ruleHitModel.setRuleDesc("年级接近");
        boolean hit = requestUser != null
                && candidateUser != null
                && requestUser.getGrade() != null
                && candidateUser.getGrade() != null
                && Math.abs(requestUser.getGrade() - candidateUser.getGrade()) <= 1;
        ruleHitModel.setHit(hit);
        ruleHitModel.setAdjustScore(hit ? BigDecimal.valueOf(0.05D) : BigDecimal.ZERO);
        return ruleHitModel;
    }
}
