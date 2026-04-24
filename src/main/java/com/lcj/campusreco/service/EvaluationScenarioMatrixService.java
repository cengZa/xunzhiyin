package com.lcj.campusreco.service;

import com.lcj.campusreco.domain.vo.EvaluationScenarioExportVO;
import java.math.BigDecimal;
import java.util.List;

public interface EvaluationScenarioMatrixService {

    EvaluationScenarioExportVO exportScenarioMatrix(List<String> scenarioModes,
                                                    List<Integer> topKValues,
                                                    List<Integer> profileTopTagCounts,
                                                    List<BigDecimal> rerankWeightScales);
}
