package com.lcj.campusreco.service;

import com.lcj.campusreco.domain.vo.ScalabilityEvaluationExportVO;
import java.util.List;

public interface ScalabilityEvaluationService {

    ScalabilityEvaluationExportVO exportScalabilityMatrix(List<Integer> userCounts,
                                                          Integer topK,
                                                          String scenarioMode);
}
