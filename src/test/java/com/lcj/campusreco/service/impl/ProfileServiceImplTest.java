package com.lcj.campusreco.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lcj.campusreco.domain.entity.TagEntity;
import com.lcj.campusreco.domain.entity.UserProfileEntity;
import com.lcj.campusreco.domain.entity.UserTagRelationEntity;
import com.lcj.campusreco.infra.redis.ProfileCacheRepository;
import com.lcj.campusreco.mapper.TagMapper;
import com.lcj.campusreco.mapper.UserProfileMapper;
import com.lcj.campusreco.mapper.UserTagRelationMapper;
import com.lcj.campusreco.strategy.profile.ImprovedTfIdfProfileWeightCalculator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private ProfileCacheRepository profileCacheRepository;
    @Mock
    private UserTagRelationMapper userTagRelationMapper;
    @Mock
    private TagMapper tagMapper;
    @Mock
    private UserProfileMapper userProfileMapper;

    @InjectMocks
    private ImprovedTfIdfProfileWeightCalculator profileWeightCalculator;

    @Test
    void buildProfileCreatesVectorAndPersistsSnapshot() {
        ProfileServiceImpl service = new ProfileServiceImpl(
                profileCacheRepository,
                userTagRelationMapper,
                tagMapper,
                userProfileMapper,
                profileWeightCalculator
        );

        UserTagRelationEntity relation1 = new UserTagRelationEntity();
        relation1.setUserId(1L);
        relation1.setTagId(101L);
        relation1.setWeightSeed(BigDecimal.ONE);
        relation1.setSelectedAt(LocalDateTime.now());

        UserTagRelationEntity relation2 = new UserTagRelationEntity();
        relation2.setUserId(1L);
        relation2.setTagId(102L);
        relation2.setWeightSeed(BigDecimal.valueOf(1.2D));
        relation2.setSelectedAt(LocalDateTime.now());

        TagEntity tag1 = new TagEntity();
        tag1.setId(101L);
        tag1.setTagName("music");
        tag1.setTagType("hobby");

        TagEntity tag2 = new TagEntity();
        tag2.setId(102L);
        tag2.setTagName("java");
        tag2.setTagType("academic");

        when(userTagRelationMapper.selectList(any())).thenReturn(List.of(relation1, relation2));
        when(tagMapper.selectBatchIds(any())).thenReturn(List.of(tag1, tag2));
        when(userProfileMapper.selectOne(any())).thenReturn(null);

        var profile = service.buildProfile(1L, "init");

        assertEquals(1L, profile.getUserId());
        assertEquals(1, profile.getProfileVersion());
        assertEquals(2, profile.getVector().size());
        assertFalse(profile.getTopKTags().isEmpty());
        verify(userProfileMapper).insert(any(UserProfileEntity.class));
        verify(profileCacheRepository).save(1L, profile);
    }
}
