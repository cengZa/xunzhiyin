package com.lcj.campusreco.domain.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class EvaluationScenarioExportVO {

    private String fileName;
    private String filePath;
    private Integer scenarioCount;
    private List<String> scenarioModes = new ArrayList<>();
    private List<Integer> topKValues = new ArrayList<>();
    private List<Integer> profileTopTagCounts = new ArrayList<>();
    private List<BigDecimal> rerankWeightScales = new ArrayList<>();
}
