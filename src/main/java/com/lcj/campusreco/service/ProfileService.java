package com.lcj.campusreco.service;

import com.lcj.campusreco.domain.model.UserProfileModel;

public interface ProfileService {

    UserProfileModel buildProfile(Long userId, String updatedBy);

    UserProfileModel getProfile(Long userId);

    void rebuildProfile(Long userId, String updatedBy);
}
