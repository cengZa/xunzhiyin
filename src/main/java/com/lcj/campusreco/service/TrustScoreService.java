package com.lcj.campusreco.service;

import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.model.TrustScoreResult;

public interface TrustScoreService {

    TrustScoreResult evaluate(UserEntity candidateUser);
}
