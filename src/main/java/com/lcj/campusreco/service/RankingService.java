package com.lcj.campusreco.service;

import com.lcj.campusreco.domain.model.RankingCandidateModel;
import java.util.List;
import java.util.Set;

public interface RankingService {

    List<RankingCandidateModel> rank(Long requestUserId, Set<Long> candidateUserIds);
}
