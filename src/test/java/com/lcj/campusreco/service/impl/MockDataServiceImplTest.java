package com.lcj.campusreco.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lcj.campusreco.domain.entity.TagEntity;
import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.entity.UserTagRelationEntity;
import com.lcj.campusreco.infra.redis.RecallIndexRepository;
import com.lcj.campusreco.mapper.TagMapper;
import com.lcj.campusreco.mapper.UserMapper;
import com.lcj.campusreco.mapper.UserTagRelationMapper;
import com.lcj.campusreco.service.ProfileService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MockDataServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private TagMapper tagMapper;
    @Mock
    private UserTagRelationMapper userTagRelationMapper;
    @Mock
    private ProfileService profileService;
    @Mock
    private RecallIndexRepository recallIndexRepository;

    @Test
    void initMockDataSeedsUsersTagsRelationsAndRebuildsArtifacts() {
        when(tagMapper.selectById(any())).thenReturn(null);
        when(userMapper.selectById(any())).thenReturn(null);
        when(userTagRelationMapper.selectOne(any())).thenReturn(null);

        UserEntity activeUser = new UserEntity();
        activeUser.setId(2001L);
        activeUser.setStatus(1);
        when(userMapper.selectList(any())).thenReturn(List.of(activeUser));

        TagEntity activeTag = new TagEntity();
        activeTag.setId(1001L);
        activeTag.setStatus(1);
        when(tagMapper.selectList(any())).thenReturn(List.of(activeTag));
        when(userTagRelationMapper.selectList(any())).thenReturn(List.of(new UserTagRelationEntity()));

        MockDataServiceImpl service = new MockDataServiceImpl(
                userMapper,
                tagMapper,
                userTagRelationMapper,
                profileService,
                recallIndexRepository
        );

        var result = service.initMockData();

        assertEquals(12, result.get("tagCount"));
        assertEquals(12, result.get("userCount"));
        assertEquals(48, result.get("relationCount"));
        verify(tagMapper, atLeastOnce()).insert(any(TagEntity.class));
        verify(userMapper, atLeastOnce()).insert(any(UserEntity.class));
        verify(userTagRelationMapper, atLeastOnce()).insert(any(UserTagRelationEntity.class));
        verify(profileService, atLeastOnce()).rebuildProfile(any(), any());
        verify(recallIndexRepository, atLeastOnce()).replaceCandidateUserIdsByTag(any(), any());
    }
}
