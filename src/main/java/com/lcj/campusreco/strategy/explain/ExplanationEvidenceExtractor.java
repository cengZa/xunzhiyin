package com.lcj.campusreco.strategy.explain;

import com.lcj.campusreco.domain.model.RankingCandidateModel;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ExplanationEvidenceExtractor {

    public Map<String, Object> extract(RankingCandidateModel candidate) {
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("contributions", candidate == null ? null : candidate.getContributions());
        evidence.put("ruleHits", candidate == null ? null : candidate.getRuleHits());
        evidence.put("scenarioMode", candidate == null ? null : candidate.getScenarioMode());
        evidence.put("scenarioLabel", candidate == null ? null : candidate.getScenarioLabel());
        evidence.put("interestScore", candidate == null ? null : candidate.getInterestScore());
        evidence.put("campusScore", candidate == null ? null : candidate.getCampusScore());
        evidence.put("trustScore", candidate == null ? null : candidate.getTrustScore());
        evidence.put("trustReasons", candidate == null ? null : candidate.getTrustReasons());
        evidence.put("exploration", candidate != null && candidate.isExploration());
        evidence.put("explorationScore", candidate == null ? null : candidate.getExplorationScore());
        evidence.put("explorationReason", candidate == null ? null : candidate.getExplorationReason());
        return evidence;
    }
}
