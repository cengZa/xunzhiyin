package com.lcj.campusreco.service;

import com.lcj.campusreco.domain.vo.EvaluationExportVO;

public interface EvaluationSnapshotService {

    EvaluationExportVO exportLatestReport(Integer topK);
}
