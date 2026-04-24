package com.lcj.campusreco.domain.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class DemoCandidateSpotlightVO {

    private Long candidateUserId;
    private String candidateNickname;
    private String storyReason;
    private List<String> highlightTags = new ArrayList<>();
}
