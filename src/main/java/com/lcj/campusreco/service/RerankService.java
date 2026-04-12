package com.lcj.campusreco.service;

import com.lcj.campusreco.domain.model.RankingCandidateModel;
import java.util.List;

public interface RerankService {

    List<RankingCandidateModel> rerank(Long requestUserId, List<RankingCandidateModel> rankingList);
}
