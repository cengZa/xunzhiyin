package com.lcj.campusreco.strategy.rerank;

import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.RuleHitModel;

public interface RerankRule {

    RuleHitModel apply(UserEntity requestUser, UserEntity candidateUser, RankingCandidateModel candidateModel);
}
