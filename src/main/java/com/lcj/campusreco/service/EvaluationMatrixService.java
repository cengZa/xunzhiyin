package com.lcj.campusreco.service;

import com.lcj.campusreco.domain.vo.EvaluationMatrixExportVO;
import java.util.List;

public interface EvaluationMatrixService {

    EvaluationMatrixExportVO exportTopKMatrix(List<Integer> topKValues);
}
