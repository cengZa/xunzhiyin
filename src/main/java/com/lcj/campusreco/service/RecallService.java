package com.lcj.campusreco.service;

import com.lcj.campusreco.domain.model.UserProfileModel;
import java.util.Set;

public interface RecallService {

    Set<Long> recallCandidateUserIds(UserProfileModel profile);
}
