package com.lcj.campusreco.domain.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class DemoStoryVO {

    private Long demoUserId;
    private String scenarioMode;
    private String scenarioLabel;
    private String storyTitle;
    private String personaSummary;
    private String storyNarrative;
    private List<String> algorithmHighlights = new ArrayList<>();
    private List<Long> expectedCandidateIds = new ArrayList<>();
    private List<DemoCandidateSpotlightVO> candidateSpotlights = new ArrayList<>();
}
