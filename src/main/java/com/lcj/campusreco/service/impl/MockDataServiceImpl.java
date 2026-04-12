package com.lcj.campusreco.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcj.campusreco.domain.entity.TagEntity;
import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.entity.UserTagRelationEntity;
import com.lcj.campusreco.infra.redis.RecallIndexRepository;
import com.lcj.campusreco.mapper.TagMapper;
import com.lcj.campusreco.mapper.UserMapper;
import com.lcj.campusreco.mapper.UserTagRelationMapper;
import com.lcj.campusreco.service.MockDataService;
import com.lcj.campusreco.service.ProfileService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class MockDataServiceImpl implements MockDataService {

    private final UserMapper userMapper;
    private final TagMapper tagMapper;
    private final UserTagRelationMapper userTagRelationMapper;
    private final ProfileService profileService;
    private final RecallIndexRepository recallIndexRepository;

    public MockDataServiceImpl(UserMapper userMapper,
                               TagMapper tagMapper,
                               UserTagRelationMapper userTagRelationMapper,
                               ProfileService profileService,
                               RecallIndexRepository recallIndexRepository) {
        this.userMapper = userMapper;
        this.tagMapper = tagMapper;
        this.userTagRelationMapper = userTagRelationMapper;
        this.profileService = profileService;
        this.recallIndexRepository = recallIndexRepository;
    }

    @Override
    public Map<String, Object> initMockData() {
        int tagCount = ensureTags();
        int userCount = ensureUsers();
        int relationCount = ensureUserTagRelations();
        int rebuiltProfiles = rebuildAllProfiles();
        int rebuiltIndexes = rebuildRecallIndex();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("tagCount", tagCount);
        summary.put("userCount", userCount);
        summary.put("relationCount", relationCount);
        summary.put("profileRebuiltCount", rebuiltProfiles);
        summary.put("recallIndexCount", rebuiltIndexes);
        return summary;
    }

    @Override
    public int rebuildAllProfiles() {
        List<UserEntity> users = userMapper.selectList(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getStatus, 1)
        );
        for (UserEntity user : users) {
            profileService.rebuildProfile(user.getId(), "rebuild");
        }
        return users.size();
    }

    @Override
    public int rebuildRecallIndex() {
        List<TagEntity> tags = tagMapper.selectList(
                new LambdaQueryWrapper<TagEntity>().eq(TagEntity::getStatus, 1)
        );
        for (TagEntity tag : tags) {
            Set<Long> userIds = new LinkedHashSet<>(
                    userTagRelationMapper.selectList(
                                    new LambdaQueryWrapper<UserTagRelationEntity>().eq(UserTagRelationEntity::getTagId, tag.getId()))
                            .stream()
                            .map(UserTagRelationEntity::getUserId)
                            .toList()
            );
            recallIndexRepository.replaceCandidateUserIdsByTag(tag.getId(), userIds);
        }
        return tags.size();
    }

    private int ensureTags() {
        List<TagEntity> tags = List.of(
                createTag(1001L, "Java", "academic"),
                createTag(1002L, "Spring", "academic"),
                createTag(1003L, "Music", "hobby"),
                createTag(1004L, "Running", "hobby"),
                createTag(1005L, "Basketball", "club"),
                createTag(1006L, "ACM", "club"),
                createTag(1007L, "AI", "academic"),
                createTag(1008L, "Photography", "hobby"),
                createTag(1009L, "Volunteering", "club"),
                createTag(1010L, "Hiking", "hobby"),
                createTag(1011L, "Frontend", "academic"),
                createTag(1012L, "Startup", "interest")
        );
        for (TagEntity tag : tags) {
            if (tagMapper.selectById(tag.getId()) == null) {
                tagMapper.insert(tag);
            }
        }
        return tags.size();
    }

    private int ensureUsers() {
        List<UserEntity> users = List.of(
                createUser(2001L, "20230001", "Alice", 2023, "Computer Science", "Engineering", "Enjoys backend and music."),
                createUser(2002L, "20230002", "Bob", 2023, "Computer Science", "Engineering", "Focuses on Spring and ACM."),
                createUser(2003L, "20220003", "Carol", 2022, "Design", "Arts", "Likes photography and music."),
                createUser(2004L, "20230004", "Dave", 2023, "Mathematics", "Science", "Enjoys running and AI."),
                createUser(2005L, "20240005", "Eve", 2024, "Computer Science", "Engineering", "Interested in Java and AI."),
                createUser(2006L, "20220006", "Frank", 2022, "Music", "Arts", "Plays basketball and loves music."),
                createUser(2007L, "20240007", "Grace", 2024, "Data Science", "Science", "Enjoys AI, volunteering and hiking."),
                createUser(2008L, "20220008", "Henry", 2022, "Automation", "Engineering", "Likes running, basketball and Spring."),
                createUser(2009L, "20230009", "Iris", 2023, "Journalism", "Media", "Creates content around music and campus events."),
                createUser(2010L, "20240010", "Jack", 2024, "Finance", "Business", "Interested in campus startup and volunteer projects."),
                createUser(2011L, "20210011", "Kelly", 2021, "Software Engineering", "Engineering", "Frontend and Java full-stack enthusiast."),
                createUser(2012L, "20230012", "Leo", 2023, "Architecture", "Architecture", "Enjoys photography, hiking and startup events.")
        );
        for (UserEntity user : users) {
            if (userMapper.selectById(user.getId()) == null) {
                userMapper.insert(user);
            }
        }
        return users.size();
    }

    private int ensureUserTagRelations() {
        List<UserTagRelationEntity> relations = List.of(
                createRelation(2001L, 1001L), createRelation(2001L, 1002L), createRelation(2001L, 1003L), createRelation(2001L, 1006L),
                createRelation(2002L, 1001L), createRelation(2002L, 1002L), createRelation(2002L, 1006L), createRelation(2002L, 1007L),
                createRelation(2003L, 1003L), createRelation(2003L, 1008L), createRelation(2003L, 1011L), createRelation(2003L, 1012L),
                createRelation(2004L, 1004L), createRelation(2004L, 1007L), createRelation(2004L, 1005L), createRelation(2004L, 1009L),
                createRelation(2005L, 1001L), createRelation(2005L, 1007L), createRelation(2005L, 1006L), createRelation(2005L, 1011L),
                createRelation(2006L, 1003L), createRelation(2006L, 1005L), createRelation(2006L, 1008L), createRelation(2006L, 1009L),
                createRelation(2007L, 1001L), createRelation(2007L, 1007L), createRelation(2007L, 1010L), createRelation(2007L, 1009L),
                createRelation(2008L, 1002L), createRelation(2008L, 1004L), createRelation(2008L, 1005L), createRelation(2008L, 1010L),
                createRelation(2009L, 1003L), createRelation(2009L, 1008L), createRelation(2009L, 1009L), createRelation(2009L, 1012L),
                createRelation(2010L, 1004L), createRelation(2010L, 1009L), createRelation(2010L, 1012L), createRelation(2010L, 1011L),
                createRelation(2011L, 1001L), createRelation(2011L, 1002L), createRelation(2011L, 1006L), createRelation(2011L, 1011L),
                createRelation(2012L, 1008L), createRelation(2012L, 1010L), createRelation(2012L, 1009L), createRelation(2012L, 1012L)
        );
        for (UserTagRelationEntity relation : relations) {
            UserTagRelationEntity exists = userTagRelationMapper.selectOne(
                    new LambdaQueryWrapper<UserTagRelationEntity>()
                            .eq(UserTagRelationEntity::getUserId, relation.getUserId())
                            .eq(UserTagRelationEntity::getTagId, relation.getTagId())
                            .last("limit 1")
            );
            if (exists == null) {
                userTagRelationMapper.insert(relation);
            }
        }
        return relations.size();
    }

    private TagEntity createTag(Long id, String name, String type) {
        TagEntity tag = new TagEntity();
        tag.setId(id);
        tag.setTagName(name);
        tag.setTagType(type);
        tag.setTagDesc(name + " mock tag");
        tag.setStatus(1);
        tag.setCreatedAt(LocalDateTime.now());
        tag.setUpdatedAt(LocalDateTime.now());
        return tag;
    }

    private UserEntity createUser(Long id, String studentNo, String nickname, Integer grade, String major, String college, String bio) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setStudentNo(studentNo);
        user.setNickname(nickname);
        user.setGender(0);
        user.setGrade(grade);
        user.setMajor(major);
        user.setCollege(college);
        user.setBio(bio);
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private UserTagRelationEntity createRelation(Long userId, Long tagId) {
        UserTagRelationEntity relation = new UserTagRelationEntity();
        relation.setUserId(userId);
        relation.setTagId(tagId);
        relation.setSourceType("manual");
        relation.setSelectedAt(LocalDateTime.now());
        relation.setWeightSeed(BigDecimal.ONE);
        relation.setCreatedAt(LocalDateTime.now());
        relation.setUpdatedAt(LocalDateTime.now());
        return relation;
    }
}
