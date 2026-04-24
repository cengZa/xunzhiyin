package com.lcj.campusreco.service;

import com.lcj.campusreco.domain.model.RankingCandidateModel;
import java.util.List;

public interface ExplorationService {

    List<RankingCandidateModel> apply(Long requestUserId,
                                      List<RankingCandidateModel> rerankedList,
                                      int topK,
                                      String scenarioMode);
}
