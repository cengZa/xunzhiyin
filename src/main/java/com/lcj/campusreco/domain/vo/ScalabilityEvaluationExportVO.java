package com.lcj.campusreco.domain.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ScalabilityEvaluationExportVO {

    private String fileName;
    private String filePath;
    private Integer experimentCount;
    private Integer topK;
    private String scenarioMode;
    private List<Integer> userCounts = new ArrayList<>();
}
