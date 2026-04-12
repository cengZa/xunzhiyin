package com.lcj.campusreco.service;

import com.lcj.campusreco.domain.vo.EvaluationSummaryVO;

public interface EvaluationService {

    EvaluationSummaryVO generateSummary(Integer topK);

    String generateMarkdownReport(Integer topK);
}
