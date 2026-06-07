package com.lcj.campusreco.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcj.campusreco.config.RecommendationTuningContext;
import com.lcj.campusreco.domain.entity.TagEntity;
import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.entity.UserProfileEntity;
import com.lcj.campusreco.domain.entity.UserTagRelationEntity;
import com.lcj.campusreco.domain.model.TagWeightModel;
import com.lcj.campusreco.domain.model.UserProfileModel;
import com.lcj.campusreco.infra.redis.ProfileCacheRepository;
import com.lcj.campusreco.mapper.TagMapper;
import com.lcj.campusreco.mapper.UserMapper;
import com.lcj.campusreco.mapper.UserProfileMapper;
import com.lcj.campusreco.mapper.UserTagRelationMapper;
import com.lcj.campusreco.service.ProfileService;
import com.lcj.campusreco.strategy.profile.ProfileWeightCalculator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final ProfileCacheRepository profileCacheRepository;
    private final UserMapper userMapper;
    private final UserTagRelationMapper userTagRelationMapper;
    private final TagMapper tagMapper;
    private final UserProfileMapper userProfileMapper;
    private final ProfileWeightCalculator profileWeightCalculator;
    private final RecommendationTuningContext tuningContext;

    public ProfileServiceImpl(ProfileCacheRepository profileCacheRepository,
                              UserMapper userMapper,
                              UserTagRelationMapper userTagRelationMapper,
                              TagMapper tagMapper,
                              UserProfileMapper userProfileMapper,
                              ProfileWeightCalculator profileWeightCalculator,
                              RecommendationTuningContext tuningContext) {
        this.profileCacheRepository = profileCacheRepository;
        this.userMapper = userMapper;
        this.userTagRelationMapper = userTagRelationMapper;
        this.tagMapper = tagMapper;
        this.userProfileMapper = userProfileMapper;
        this.profileWeightCalculator = profileWeightCalculator;
        this.tuningContext = tuningContext;
    }

    @Override
    public UserProfileModel buildProfile(Long userId, String updatedBy) {
        List<UserTagRelationEntity> relations = userTagRelationMapper.selectList(
                new LambdaQueryWrapper<UserTagRelationEntity>()
                        .eq(UserTagRelationEntity::getUserId, userId)
                        .orderByDesc(UserTagRelationEntity::getSelectedAt)
                        .orderByDesc(UserTagRelationEntity::getUpdatedAt)
        );
        List<Long> tagIds = relations.stream()
                .map(UserTagRelationEntity::getTagId)
                .distinct()
                .toList();
        List<TagEntity> tags = tagIds.isEmpty() ? List.of() : tagMapper.selectBatchIds(tagIds);
        CorpusStats corpusStats = buildCorpusStats();
        List<TagWeightModel> tagWeights = new ArrayList<>(
                profileWeightCalculator.calculateWeights(
                        relations,
                        tags,
                        corpusStats.tagDocumentFrequencies(),
                        corpusStats.activeUserCount()
                )
        );
        tagWeights.sort(Comparator.comparing(TagWeightModel::getFinalWeight).reversed());

        UserProfileModel profileModel = new UserProfileModel();
        profileModel.setUserId(userId);
        profileModel.setTagWeights(tagWeights);
        profileModel.setTopKTags(new ArrayList<>(tagWeights.stream()
                .limit(tuningContext.getProfileTopTagLimit())
                .toList()));
        profileModel.setVector(tagWeights.stream().collect(java.util.stream.Collectors.toMap(
                TagWeightModel::getTagId,
                TagWeightModel::getFinalWeight,
                (left, right) -> right
        )));
        int nextVersion = getLatestProfileVersion(userId) + 1;
        profileModel.setProfileVersion(nextVersion);

        UserProfileEntity profileEntity = new UserProfileEntity();
        profileEntity.setUserId(userId);
        profileEntity.setProfileVersion(nextVersion);
        profileEntity.setProfileJson(serializeVector(profileModel.getVector()));
        profileEntity.setTopkJson(serializeTopK(profileModel.getTopKTags()));
        profileEntity.setUpdatedBy(updatedBy);
        profileEntity.setCreatedAt(LocalDateTime.now());
        profileEntity.setUpdatedAt(LocalDateTime.now());
        userProfileMapper.insert(profileEntity);

        profileCacheRepository.save(userId, profileModel);
        return profileModel;
    }

    private CorpusStats buildCorpusStats() {
        List<UserEntity> activeUsers = userMapper.selectList(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getStatus, 1)
        );
        Set<Long> activeUserIds = new HashSet<>();
        for (UserEntity user : activeUsers) {
            activeUserIds.add(user.getId());
        }
        if (activeUserIds.isEmpty()) {
            return new CorpusStats(Map.of(), 0);
        }

        List<UserTagRelationEntity> allRelations = userTagRelationMapper.selectList(
                new LambdaQueryWrapper<UserTagRelationEntity>().in(UserTagRelationEntity::getUserId, activeUserIds)
        );
        Map<Long, Set<Long>> usersByTag = new HashMap<>();
        for (UserTagRelationEntity relation : allRelations) {
            if (relation.getTagId() == null || relation.getUserId() == null) {
                continue;
            }
            usersByTag.computeIfAbsent(relation.getTagId(), ignored -> new HashSet<>()).add(relation.getUserId());
        }

        Map<Long, Long> tagDocumentFrequencies = new HashMap<>();
        for (Map.Entry<Long, Set<Long>> entry : usersByTag.entrySet()) {
            tagDocumentFrequencies.put(entry.getKey(), (long) entry.getValue().size());
        }
        return new CorpusStats(tagDocumentFrequencies, activeUserIds.size());
    }

    @Override
    public UserProfileModel getProfile(Long userId) {
        UserProfileModel cached = profileCacheRepository.get(userId);
        if (cached != null) {
            return cached;
        }
        List<UserTagRelationEntity> relations = userTagRelationMapper.selectList(
                new LambdaQueryWrapper<UserTagRelationEntity>().eq(UserTagRelationEntity::getUserId, userId)
        );
        if (relations.isEmpty()) {
            UserProfileModel emptyProfile = new UserProfileModel();
            emptyProfile.setUserId(userId);
            emptyProfile.setProfileVersion(1);
            return emptyProfile;
        }
        return buildProfile(userId, "rebuild");
    }

    @Override
    public void rebuildProfile(Long userId, String updatedBy) {
        profileCacheRepository.evict(userId);
        buildProfile(userId, updatedBy);
    }

    private int getLatestProfileVersion(Long userId) {
        UserProfileEntity latest = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfileEntity>()
                        .eq(UserProfileEntity::getUserId, userId)
                        .orderByDesc(UserProfileEntity::getProfileVersion)
                        .last("limit 1")
        );
        return latest == null || latest.getProfileVersion() == null ? 0 : latest.getProfileVersion();
    }

    private String serializeVector(Map<Long, java.math.BigDecimal> vector) {
        return vector.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(java.util.stream.Collectors.joining(","));
    }

    private String serializeTopK(List<TagWeightModel> topKTags) {
        return topKTags.stream()
                .map(tag -> tag.getTagId() + "|" + tag.getTagName() + "|" + tag.getFinalWeight())
                .collect(java.util.stream.Collectors.joining(","));
    }

    private record CorpusStats(Map<Long, Long> tagDocumentFrequencies, long activeUserCount) {
    }
}
