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
        return evidence;
    }
}
