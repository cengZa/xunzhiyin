package com.lcj.campusreco.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcj.campusreco.common.constant.FeedbackType;
import com.lcj.campusreco.domain.dto.FeedbackSubmitDTO;
import com.lcj.campusreco.domain.entity.TagEntity;
import com.lcj.campusreco.domain.entity.UserFeedbackEntity;
import com.lcj.campusreco.domain.entity.UserTagRelationEntity;
import com.lcj.campusreco.mapper.UserFeedbackMapper;
import com.lcj.campusreco.mapper.UserTagRelationMapper;
import com.lcj.campusreco.service.FeedbackService;
import com.lcj.campusreco.service.ProfileService;
import com.lcj.campusreco.service.TagService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    private final UserFeedbackMapper userFeedbackMapper;
    private final UserTagRelationMapper userTagRelationMapper;
    private final TagService tagService;
    private final ProfileService profileService;

    public FeedbackServiceImpl(UserFeedbackMapper userFeedbackMapper,
                               UserTagRelationMapper userTagRelationMapper,
                               TagService tagService,
                               ProfileService profileService) {
        this.userFeedbackMapper = userFeedbackMapper;
        this.userTagRelationMapper = userTagRelationMapper;
        this.tagService = tagService;
        this.profileService = profileService;
    }

    @Override
    public void submitFeedback(Long requestUserId, FeedbackSubmitDTO dto) {
        UserFeedbackEntity userFeedbackEntity = new UserFeedbackEntity();
        userFeedbackEntity.setRequestUserId(requestUserId);
        userFeedbackEntity.setRecommendationId(dto.getRecommendationId());
        userFeedbackEntity.setTargetUserId(dto.getTargetUserId());
        userFeedbackEntity.setFeedbackType(dto.getFeedbackType());
        userFeedbackEntity.setFeedbackTime(LocalDateTime.now());
        userFeedbackEntity.setCreatedAt(LocalDateTime.now());
        userFeedbackMapper.insert(userFeedbackEntity);
        applyFeedbackUpdate(requestUserId, dto.getTargetUserId(), dto.getFeedbackType());
    }

    @Override
    public void applyFeedbackUpdate(Long requestUserId, Long recommendationId, String feedbackType) {
        List<TagEntity> targetTags = tagService.listUserTags(recommendationId);
        for (TagEntity targetTag : targetTags) {
            UserTagRelationEntity existingRelation = userTagRelationMapper.selectOne(
                    new LambdaQueryWrapper<UserTagRelationEntity>()
                            .eq(UserTagRelationEntity::getUserId, requestUserId)
                            .eq(UserTagRelationEntity::getTagId, targetTag.getId())
                            .last("limit 1")
            );
            if (FeedbackType.FOLLOW.equalsIgnoreCase(feedbackType)) {
                if (existingRelation == null) {
                    UserTagRelationEntity relationEntity = new UserTagRelationEntity();
                    relationEntity.setUserId(requestUserId);
                    relationEntity.setTagId(targetTag.getId());
                    relationEntity.setSourceType("feedback");
                    relationEntity.setSelectedAt(LocalDateTime.now());
                    relationEntity.setWeightSeed(BigDecimal.valueOf(0.5D));
                    relationEntity.setCreatedAt(LocalDateTime.now());
                    relationEntity.setUpdatedAt(LocalDateTime.now());
                    userTagRelationMapper.insert(relationEntity);
                } else {
                    existingRelation.setWeightSeed((existingRelation.getWeightSeed() == null ? BigDecimal.ONE : existingRelation.getWeightSeed())
                            .multiply(BigDecimal.valueOf(1.1D)));
                    existingRelation.setUpdatedAt(LocalDateTime.now());
                    existingRelation.setSelectedAt(LocalDateTime.now());
                    userTagRelationMapper.updateById(existingRelation);
                }
            } else if (existingRelation != null) {
                existingRelation.setWeightSeed((existingRelation.getWeightSeed() == null ? BigDecimal.ONE : existingRelation.getWeightSeed())
                        .multiply(BigDecimal.valueOf(0.8D)));
                existingRelation.setUpdatedAt(LocalDateTime.now());
                userTagRelationMapper.updateById(existingRelation);
            }
        }
        profileService.rebuildProfile(requestUserId, "feedback");
    }

    @Override
    public List<UserFeedbackEntity> listByUserId(Long userId) {
        return userFeedbackMapper.selectList(
                new LambdaQueryWrapper<UserFeedbackEntity>()
                        .eq(UserFeedbackEntity::getRequestUserId, userId)
                        .orderByDesc(UserFeedbackEntity::getFeedbackTime)
        );
    }
}
