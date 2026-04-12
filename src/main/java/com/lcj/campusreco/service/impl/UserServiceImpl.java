package com.lcj.campusreco.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcj.campusreco.domain.dto.UserCreateDTO;
import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.mapper.UserMapper;
import com.lcj.campusreco.service.UserService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Long createUser(UserCreateDTO dto) {
        UserEntity userEntity = new UserEntity();
        userEntity.setStudentNo(dto.getStudentNo());
        userEntity.setNickname(dto.getNickname());
        userEntity.setGender(dto.getGender());
        userEntity.setGrade(dto.getGrade());
        userEntity.setMajor(dto.getMajor());
        userEntity.setCollege(dto.getCollege());
        userEntity.setBio(dto.getBio());
        userEntity.setStatus(1);
        userEntity.setCreatedAt(LocalDateTime.now());
        userEntity.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(userEntity);
        return userEntity.getId();
    }

    @Override
    public UserEntity getById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public List<UserEntity> listByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userMapper.selectList(new LambdaQueryWrapper<UserEntity>().in(UserEntity::getId, userIds));
    }
}
