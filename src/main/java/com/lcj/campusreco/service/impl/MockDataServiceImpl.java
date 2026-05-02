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

    private static final List<Long> MOCK_USER_IDS = List.of(
            2001L, 2002L, 2003L, 2004L, 2005L, 2006L, 2007L, 2008L, 2009L,
            2010L, 2011L, 2012L, 2013L, 2014L, 2015L, 2016L, 2017L, 2018L
    );
    private static final List<Long> MOCK_TAG_IDS = List.of(
            1001L, 1002L, 1003L, 1004L, 1005L, 1006L, 1007L, 1008L, 1009L,
            1010L, 1011L, 1012L, 1013L, 1014L, 1015L, 1016L, 1017L, 1018L
    );

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
                                    new LambdaQueryWrapper<UserTagRelationEntity>()
                                            .eq(UserTagRelationEntity::getTagId, tag.getId()))
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
                createTag(1012L, "Startup", "interest"),
                createTag(1013L, "MachineLearning", "academic"),
                createTag(1014L, "Badminton", "hobby"),
                createTag(1015L, "Debate", "club"),
                createTag(1016L, "StudentUnion", "club"),
                createTag(1017L, "Guitar", "hobby"),
                createTag(1018L, "DataMining", "academic")
        );
        for (TagEntity tag : tags) {
            TagEntity existing = tagMapper.selectById(tag.getId());
            if (existing == null) {
                tagMapper.insert(tag);
            } else {
                tag.setCreatedAt(existing.getCreatedAt());
                tagMapper.updateById(tag);
            }
        }
        return tags.size();
    }

    private int ensureUsers() {
        List<UserEntity> users = List.of(
                createUser(2001L, "20230001", "林舟", 2023, "Computer Science", "Engineering", "主讲演示用户，偏好 Java、Spring、音乐、ACM 和羽毛球。"),
                createUser(2002L, "20230002", "柏宇", 2023, "Computer Science", "Engineering", "学习搭子第一候选，技术社团活跃，擅长 Java、Spring、ACM 与 AI 协作。"),
                createUser(2003L, "20220003", "陈栀", 2022, "Design", "Arts", "兴趣同频候选，摄影、音乐和前端设计并重，适合展示跨专业兴趣匹配。"),
                createUser(2004L, "20230004", "段可", 2023, "Mathematics", "Science", "兼顾 AI、志愿活动和辩论，适合展示学习与社团规则交叉命中。"),
                createUser(2005L, "20240005", "顾宁", 2024, "Computer Science", "Engineering", "跨年级技术候选，偏好 Java、AI、机器学习和前端。"),
                createUser(2006L, "20220006", "何川", 2022, "Music", "Arts", "音乐、摄影、羽毛球和吉他都很活跃，适合纯兴趣型推荐演示。"),
                createUser(2007L, "20240007", "姜遥", 2024, "Data Science", "Science", "数据科学方向，兼顾志愿活动、徒步和机器学习。"),
                createUser(2008L, "20220008", "康唐", 2022, "Automation", "Engineering", "运动社团活跃，跑步、篮球、羽毛球和工程技术结合明显。"),
                createUser(2009L, "20230009", "罗栀", 2023, "Journalism", "Media", "校园活动运营向用户，音乐、摄影、志愿活动和创业兴趣并重。"),
                createUser(2010L, "20240010", "孟夏", 2024, "Finance", "Business", "学生组织和创业活动参与度高，适合作为社团搭子场景候选。"),
                createUser(2011L, "20210011", "乔溪", 2021, "Software Engineering", "Engineering", "高质量技术候选，Java、Spring、ACM 和机器学习非常稳定。"),
                createUser(2012L, "20230012", "沈原", 2023, "Architecture", "Architecture", "摄影、徒步、学生会和吉他并重，适合展示跨圈层兴趣连接。"),
                createUser(2013L, "20220013", "苏衡", 2022, "Electronic Information", "Engineering", "学习搭子第二梯队，AI、数据挖掘和辩论能力突出。"),
                createUser(2014L, "20230014", "唐澈", 2023, "Public Administration", "Humanities", "社团搭子重点候选，学生会、志愿活动、辩论和运动连接都很强。"),
                createUser(2015L, "20240015", "周霁", 2024, "Digital Media", "Arts", "兴趣同频重点候选，音乐、摄影、吉他和羽毛球共同构成高解释性案例。"),
                createUser(2016L, "20230016", "许岚", 2023, "Industrial Design", "Design", "产品与创业兴趣明显，兼顾前端、摄影和学生组织。"),
                createUser(2017L, "20240017", "魏澄", 2024, "Statistics", "Science", "AI 和数据分析。"),
                createUser(2018L, "20230018", "陆闻", 2023, "Management", "Business", "")
        );
        for (UserEntity user : users) {
            UserEntity existing = userMapper.selectById(user.getId());
            if (existing == null) {
                userMapper.insert(user);
            } else {
                user.setCreatedAt(existing.getCreatedAt());
                userMapper.updateById(user);
            }
        }
        return users.size();
    }

    private int ensureUserTagRelations() {
        List<UserTagRelationEntity> relations = List.of(
                createRelation(2001L, 1001L), createRelation(2001L, 1002L), createRelation(2001L, 1003L), createRelation(2001L, 1006L), createRelation(2001L, 1014L),
                createRelation(2002L, 1001L), createRelation(2002L, 1002L), createRelation(2002L, 1006L), createRelation(2002L, 1007L), createRelation(2002L, 1018L),
                createRelation(2003L, 1003L), createRelation(2003L, 1008L), createRelation(2003L, 1011L), createRelation(2003L, 1012L), createRelation(2003L, 1017L),
                createRelation(2004L, 1007L), createRelation(2004L, 1009L), createRelation(2004L, 1004L), createRelation(2004L, 1015L), createRelation(2004L, 1018L),
                createRelation(2005L, 1001L), createRelation(2005L, 1007L), createRelation(2005L, 1011L), createRelation(2005L, 1013L), createRelation(2005L, 1018L),
                createRelation(2006L, 1003L), createRelation(2006L, 1008L), createRelation(2006L, 1005L), createRelation(2006L, 1014L), createRelation(2006L, 1017L),
                createRelation(2007L, 1007L), createRelation(2007L, 1009L), createRelation(2007L, 1010L), createRelation(2007L, 1013L), createRelation(2007L, 1018L),
                createRelation(2008L, 1002L), createRelation(2008L, 1004L), createRelation(2008L, 1005L), createRelation(2008L, 1010L), createRelation(2008L, 1014L),
                createRelation(2009L, 1003L), createRelation(2009L, 1008L), createRelation(2009L, 1009L), createRelation(2009L, 1012L), createRelation(2009L, 1016L),
                createRelation(2010L, 1009L), createRelation(2010L, 1012L), createRelation(2010L, 1015L), createRelation(2010L, 1016L), createRelation(2010L, 1017L),
                createRelation(2011L, 1001L), createRelation(2011L, 1002L), createRelation(2011L, 1006L), createRelation(2011L, 1011L), createRelation(2011L, 1013L),
                createRelation(2012L, 1008L), createRelation(2012L, 1010L), createRelation(2012L, 1012L), createRelation(2012L, 1016L), createRelation(2012L, 1017L),
                createRelation(2013L, 1001L), createRelation(2013L, 1007L), createRelation(2013L, 1013L), createRelation(2013L, 1015L), createRelation(2013L, 1018L),
                createRelation(2014L, 1005L), createRelation(2014L, 1009L), createRelation(2014L, 1014L), createRelation(2014L, 1015L), createRelation(2014L, 1016L),
                createRelation(2015L, 1003L), createRelation(2015L, 1008L), createRelation(2015L, 1012L), createRelation(2015L, 1014L), createRelation(2015L, 1017L),
                createRelation(2016L, 1008L), createRelation(2016L, 1011L), createRelation(2016L, 1012L), createRelation(2016L, 1016L), createRelation(2016L, 1017L),
                createRelation(2017L, 1007L), createRelation(2017L, 1013L), createRelation(2017L, 1018L),
                createRelation(2018L, 1009L), createRelation(2018L, 1012L), createRelation(2018L, 1016L)
        );

        userTagRelationMapper.delete(
                new LambdaQueryWrapper<UserTagRelationEntity>().in(UserTagRelationEntity::getUserId, MOCK_USER_IDS)
        );
        for (UserTagRelationEntity relation : relations) {
            userTagRelationMapper.insert(relation);
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

    private UserEntity createUser(Long id,
                                  String studentNo,
                                  String nickname,
                                  Integer grade,
                                  String major,
                                  String college,
                                  String bio) {
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
