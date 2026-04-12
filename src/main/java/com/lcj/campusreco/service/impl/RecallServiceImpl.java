package com.lcj.campusreco.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcj.campusreco.domain.entity.UserTagRelationEntity;
import com.lcj.campusreco.domain.model.TagWeightModel;
import com.lcj.campusreco.domain.model.UserProfileModel;
import com.lcj.campusreco.infra.redis.RecallIndexRepository;
import com.lcj.campusreco.mapper.UserTagRelationMapper;
import com.lcj.campusreco.service.RecallService;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RecallServiceImpl implements RecallService {

    private final RecallIndexRepository recallIndexRepository;
    private final UserTagRelationMapper userTagRelationMapper;

    public RecallServiceImpl(RecallIndexRepository recallIndexRepository, UserTagRelationMapper userTagRelationMapper) {
        this.recallIndexRepository = recallIndexRepository;
        this.userTagRelationMapper = userTagRelationMapper;
    }

    @Override
    public Set<Long> recallCandidateUserIds(UserProfileModel profile) {
        Set<Long> candidateUserIds = new LinkedHashSet<>();
        for (TagWeightModel tagWeight : profile.getTopKTags()) {
            Set<String> redisCandidates = recallIndexRepository.getCandidateUserIdsByTag(tagWeight.getTagId());
            if (!redisCandidates.isEmpty()) {
                redisCandidates
                    .stream()
                    .map(Long::valueOf)
                    .filter(candidateUserId -> !candidateUserId.equals(profile.getUserId()))
                    .forEach(candidateUserIds::add);
                continue;
            }
            userTagRelationMapper.selectList(
                            new LambdaQueryWrapper<UserTagRelationEntity>().eq(UserTagRelationEntity::getTagId, tagWeight.getTagId()))
                    .stream()
                    .map(UserTagRelationEntity::getUserId)
                    .filter(candidateUserId -> !candidateUserId.equals(profile.getUserId()))
                    .forEach(candidateUserIds::add);
        }
        return candidateUserIds;
    }
}
