package com.lcj.campusreco.strategy.rerank;

import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.model.ContributionItemModel;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.RuleHitModel;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ClubOverlapRule implements RerankRule {

    private static final Set<String> CAMPUS_SCENE_TAGS = Set.of("ACM", "Basketball", "Volunteering");

    @Override
    public RuleHitModel apply(UserEntity requestUser, UserEntity candidateUser, RankingCandidateModel candidateModel) {
        RuleHitModel ruleHitModel = new RuleHitModel();
        ruleHitModel.setRuleCode("CLUB_OVERLAP");
        ruleHitModel.setRuleDesc("校园社团兴趣重合");
        boolean hit = candidateModel != null
                && candidateModel.getContributions().stream()
                .map(ContributionItemModel::getTagName)
                .anyMatch(CAMPUS_SCENE_TAGS::contains);
        ruleHitModel.setHit(hit);
        ruleHitModel.setAdjustScore(hit ? BigDecimal.valueOf(0.04D) : BigDecimal.ZERO);
        return ruleHitModel;
    }
}
