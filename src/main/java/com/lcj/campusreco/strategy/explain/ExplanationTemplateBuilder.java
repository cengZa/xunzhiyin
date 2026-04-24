package com.lcj.campusreco.strategy.explain;

import com.lcj.campusreco.common.constant.RecommendationScenarioMode;
import com.lcj.campusreco.domain.model.ContributionItemModel;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.RuleHitModel;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ExplanationTemplateBuilder {

    public String build(RankingCandidateModel candidate) {
        if (candidate == null) {
            return "推荐原因：系统根据兴趣画像、校园场景和可信连接信号生成了当前结果。";
        }
        String scenarioLabel = RecommendationScenarioMode.labelOf(candidate.getScenarioMode());
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

        StringBuilder builder = new StringBuilder("推荐原因：在");
        builder.append(scenarioLabel).append("模式下，");
        if (!topTags.isEmpty()) {
            builder.append("你们在 ").append(String.join("、", topTags)).append(" 上更同频");
        } else {
            builder.append("你们的兴趣画像相似度较高");
        }
        if (!hitRules.isEmpty()) {
            builder.append("，并且命中了 ").append(String.join("、", hitRules));
        }
        if (candidate.getTrustReasons() != null && !candidate.getTrustReasons().isEmpty()) {
            builder.append("；可信连接信号来自 ").append(String.join("、", candidate.getTrustReasons()));
        }
        if (candidate.isExploration() && candidate.getExplorationReason() != null && !candidate.getExplorationReason().isBlank()) {
            builder.append("；该候选人作为轻量探索位被保留，因为 ").append(candidate.getExplorationReason());
        }
        return builder.toString();
    }
}
