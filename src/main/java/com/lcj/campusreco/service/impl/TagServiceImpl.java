package com.lcj.campusreco.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcj.campusreco.domain.entity.TagEntity;
import com.lcj.campusreco.domain.entity.UserTagRelationEntity;
import com.lcj.campusreco.mapper.TagMapper;
import com.lcj.campusreco.mapper.UserTagRelationMapper;
import com.lcj.campusreco.service.TagService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final UserTagRelationMapper userTagRelationMapper;

    public TagServiceImpl(TagMapper tagMapper, UserTagRelationMapper userTagRelationMapper) {
        this.tagMapper = tagMapper;
        this.userTagRelationMapper = userTagRelationMapper;
    }

    @Override
    public void bindUserTags(Long userId, List<Long> tagIds, String sourceType) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long tagId : tagIds) {
            UserTagRelationEntity relationEntity = new UserTagRelationEntity();
            relationEntity.setUserId(userId);
            relationEntity.setTagId(tagId);
            relationEntity.setSourceType(sourceType);
            relationEntity.setSelectedAt(now);
            relationEntity.setCreatedAt(now);
            relationEntity.setUpdatedAt(now);
            userTagRelationMapper.insert(relationEntity);
        }
    }

    @Override
    public List<TagEntity> listUserTags(Long userId) {
        List<Long> tagIds = userTagRelationMapper.selectList(
                        new LambdaQueryWrapper<UserTagRelationEntity>().eq(UserTagRelationEntity::getUserId, userId))
                .stream()
                .map(UserTagRelationEntity::getTagId)
                .filter(Objects::nonNull)
                .toList();
        if (tagIds.isEmpty()) {
            return Collections.emptyList();
        }
        return tagMapper.selectBatchIds(tagIds);
    }
}
