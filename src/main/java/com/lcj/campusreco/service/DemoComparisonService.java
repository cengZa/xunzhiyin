package com.lcj.campusreco.service;

import com.lcj.campusreco.domain.vo.DemoComparisonVO;

public interface DemoComparisonService {

    DemoComparisonVO compareViews(Long userId, Integer topK, String scenarioMode);
}
