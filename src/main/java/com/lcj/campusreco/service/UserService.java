package com.lcj.campusreco.service;

import com.lcj.campusreco.domain.dto.UserCreateDTO;
import com.lcj.campusreco.domain.entity.UserEntity;
import java.util.List;

public interface UserService {

    Long createUser(UserCreateDTO dto);

    UserEntity getById(Long userId);

    List<UserEntity> listByIds(List<Long> userIds);
}
