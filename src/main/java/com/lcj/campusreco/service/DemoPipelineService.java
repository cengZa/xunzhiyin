package com.lcj.campusreco.service;

import com.lcj.campusreco.domain.vo.DemoPipelineVO;

public interface DemoPipelineService {

    DemoPipelineVO buildPipeline(Long userId, Integer topK, String scenarioMode);
}
