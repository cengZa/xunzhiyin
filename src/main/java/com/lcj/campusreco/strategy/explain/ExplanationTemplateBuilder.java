package com.lcj.campusreco.strategy.explain;

import com.lcj.campusreco.domain.model.ContributionItemModel;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.RuleHitModel;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ExplanationTemplateBuilder {

    public String build(RankingCandidateModel candidate) {
        if (candidate == null) {
            return "Recommended based on profile similarity.";
        }
        List<String> topTags = candidate.getContributions().stream()
                .limit(2)
                .map(ContributionItemModel::getTagName)
                .filter(tagName -> tagName != null && !tagName.isBlank())
                .toList();
        List<String> hitRules = candidate.getRuleHits().stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getHit()))
                .map(RuleHitModel::getRuleDesc)
                .filter(ruleDesc -> ruleDesc != null && !ruleDesc.isBlank())
                .limit(2)
                .toList();

        StringBuilder builder = new StringBuilder("Recommended because ");
        if (!topTags.isEmpty()) {
            builder.append("you both share ").append(String.join(", ", topTags));
        } else {
            builder.append("your profiles are similar");
        }
        if (!hitRules.isEmpty()) {
            builder.append(", and ").append(String.join(", ", hitRules));
        }
        return builder.toString();
    }
}
