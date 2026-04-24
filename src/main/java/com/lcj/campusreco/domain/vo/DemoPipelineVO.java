package com.lcj.campusreco.domain.vo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class DemoPipelineVO {

    private Long userId;
    private Integer topK;
    private Integer recallCandidateCount;
    private String scenarioMode;
    private String scenarioLabel;
    private Map<String, Object> requestUser = new LinkedHashMap<>();
    private Map<String, Object> scenarioStage = new LinkedHashMap<>();
    private List<Map<String, Object>> inputTags = new ArrayList<>();
    private Map<String, Object> profileStage = new LinkedHashMap<>();
    private List<Map<String, Object>> recallStage = new ArrayList<>();
    private List<Map<String, Object>> rankingStage = new ArrayList<>();
    private List<Map<String, Object>> rerankStage = new ArrayList<>();
    private List<Map<String, Object>> finalStage = new ArrayList<>();
}
