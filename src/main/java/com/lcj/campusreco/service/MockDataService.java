package com.lcj.campusreco.service;

import java.util.Map;

public interface MockDataService {

    Map<String, Object> initMockData();

    int rebuildAllProfiles();

    int rebuildRecallIndex();
}
