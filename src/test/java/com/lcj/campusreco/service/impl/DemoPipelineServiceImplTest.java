package com.lcj.campusreco.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.lcj.campusreco.config.RecommendationTuningContext;
import com.lcj.campusreco.domain.entity.TagEntity;
import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.entity.UserTagRelationEntity;
import com.lcj.campusreco.domain.model.ContributionItemModel;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.TagWeightModel;
import com.lcj.campusreco.domain.model.UserProfileModel;
import com.lcj.campusreco.domain.vo.ExplanationVO;
import com.lcj.campusreco.infra.redis.RecallIndexRepository;
import com.lcj.campusreco.mapper.UserFeedbackMapper;
import com.lcj.campusreco.mapper.UserTagRelationMapper;
import com.lcj.campusreco.service.ExplorationService;
import com.lcj.campusreco.service.ExplanationService;
import com.lcj.campusreco.service.ProfileService;
import com.lcj.campusreco.service.RankingService;
import com.lcj.campusreco.service.RecallService;
import com.lcj.campusreco.service.RerankService;
import com.lcj.campusreco.service.TagService;
import com.lcj.campusreco.service.UserService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DemoPipelineServiceImplTest {

    @Mock
    private ProfileService profileService;
    @Mock
    private RecallService recallService;
    @Mock
    private RankingService rankingService;
    @Mock
    private RerankService rerankService;
    @Mock
    private ExplorationService explorationService;
    @Mock
    private ExplanationService explanationService;
    @Mock
    private UserService userService;
    @Mock
    private TagService tagService;
    @Mock
    private UserTagRelationMapper userTagRelationMapper;
    @Mock
    private UserFeedbackMapper userFeedbackMapper;
    @Mock
    private RecallIndexRepository recallIndexRepository;

    @Test
    void buildPipelineReturnsAllStages() {
        DemoPipelineServiceImpl service = new DemoPipelineServiceImpl(
                profileService,
                recallService,
                rankingService,
                rerankService,
                explorationService,
                explanationService,
                userService,
                tagService,
                new RecommendationTuningContext(5, BigDecimal.ONE, "study_partner", true),
                userTagRelationMapper,
                userFeedbackMapper,
                recallIndexRepository
        );

        UserEntity requestUser = new UserEntity();
        requestUser.setId(2001L);
        requestUser.setNickname("林同学");
        requestUser.setMajor("计算机科学");
        requestUser.setCollege("信息学院");
        requestUser.setGrade(3);

        TagEntity tag = new TagEntity();
        tag.setId(101L);
        tag.setTagName("AI");
        tag.setTagType("academic");

        UserTagRelationEntity relation = new UserTagRelationEntity();
        relation.setUserId(2001L);
        relation.setTagId(101L);
        relation.setSourceType("manual");
        relation.setWeightSeed(BigDecimal.ONE);

        TagWeightModel tagWeight = new TagWeightModel();
        tagWeight.setTagId(101L);
        tagWeight.setTagName("AI");
        tagWeight.setTf(new BigDecimal("1.0000"));
        tagWeight.setIdf(new BigDecimal("1.2000"));
        tagWeight.setTimeDecay(new BigDecimal("0.9500"));
        tagWeight.setFinalWeight(new BigDecimal("1.1400"));

        UserProfileModel profile = new UserProfileModel();
        profile.setUserId(2001L);
        profile.setProfileVersion(2);
        profile.getVector().put(101L, new BigDecimal("1.1400"));
        profile.getTagWeights().add(tagWeight);
        profile.getTopKTags().add(tagWeight);

        RankingCandidateModel ranked = new RankingCandidateModel();
        ranked.setTargetUserId(2002L);
        ranked.setRecallScore(new BigDecimal("3.0000"));
        ranked.setRankScore(new BigDecimal("0.9100"));
        ranked.setInterestScore(new BigDecimal("0.9100"));
        ranked.setCampusScore(new BigDecimal("0.0800"));
        ranked.setRerankScore(new BigDecimal("0.0800"));
        ranked.setTrustScore(new BigDecimal("0.2400"));
        ranked.setFinalScore(new BigDecimal("1.0260"));
        ContributionItemModel contribution = new ContributionItemModel();
        contribution.setTagName("AI");
        contribution.setContributionScore(new BigDecimal("0.8200"));
        ranked.getContributions().add(contribution);
        ranked.getTrustReasons().add("资料完整");

        RankingCandidateModel explored = new RankingCandidateModel();
        explored.setTargetUserId(2003L);
        explored.setRecallScore(new BigDecimal("2.0000"));
        explored.setRankScore(new BigDecimal("0.8200"));
        explored.setInterestScore(new BigDecimal("0.8200"));
        explored.setCampusScore(new BigDecimal("0.0100"));
        explored.setRerankScore(new BigDecimal("0.0100"));
        explored.setTrustScore(new BigDecimal("0.2200"));
        explored.setFinalScore(new BigDecimal("0.8630"));
        explored.setExploration(true);
        explored.setExplorationScore(new BigDecimal("0.7300"));
        explored.setExplorationReason("跨专业但兴趣标签高度重合");

        UserEntity candidateOne = new UserEntity();
        candidateOne.setId(2002L);
        candidateOne.setNickname("周同学");
        candidateOne.setMajor("软件工程");
        candidateOne.setCollege("信息学院");
        candidateOne.setGrade(3);

        UserEntity candidateTwo = new UserEntity();
        candidateTwo.setId(2003L);
        candidateTwo.setNickname("陈同学");
        candidateTwo.setMajor("数字媒体技术");
        candidateTwo.setCollege("设计学院");
        candidateTwo.setGrade(2);

        ExplanationVO explanationVO = new ExplanationVO();
        explanationVO.setReasonText("测试解释");
        explanationVO.setEvidence(java.util.Map.of("exploration", true));

        when(userService.getById(2001L)).thenReturn(requestUser);
        when(userService.listByIds(anyList())).thenReturn(List.of(candidateOne, candidateTwo));
        when(tagService.listUserTags(2001L)).thenReturn(List.of(tag));
        when(userTagRelationMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(relation));
        when(userTagRelationMapper.selectCount(org.mockito.ArgumentMatchers.any())).thenReturn(4L);
        when(userFeedbackMapper.selectCount(org.mockito.ArgumentMatchers.any())).thenReturn(2L);
        when(recallIndexRepository.getCandidateUserIdsByTag(101L)).thenReturn(Set.of("2002", "2003"));
        when(profileService.getProfile(2001L)).thenReturn(profile);
        when(recallService.recallCandidateUserIds(profile)).thenReturn(Set.of(2002L, 2003L));
        when(rankingService.rank(eq(2001L), anySet())).thenReturn(List.of(ranked, explored));
        when(rerankService.rerank(eq(2001L), anyList())).thenReturn(List.of(ranked, explored));
        when(explorationService.apply(eq(2001L), anyList(), eq(3), eq("study_partner"))).thenReturn(List.of(ranked, explored));
        when(explanationService.generate(ranked)).thenReturn(explanationVO);
        when(explanationService.generate(explored)).thenReturn(explanationVO);

        var result = service.buildPipeline(2001L, 3, "study_partner");

        assertEquals(2001L, result.getUserId());
        assertEquals("study_partner", result.getScenarioMode());
        assertEquals(1, result.getInputTags().size());
        assertFalse(result.getProfileStage().isEmpty());
        assertEquals(2, result.getRecallStage().size());
        assertEquals(2, result.getRankingStage().size());
        assertEquals(2, result.getRerankStage().size());
        assertEquals(2, result.getFinalStage().size());
        assertFalse(result.getScenarioStage().isEmpty());
        assertEquals("学习搭子", result.getScenarioStage().get("scenarioLabel"));
        assertEquals("学术", result.getInputTags().get(0).get("tagTypeLabel"));
        assertEquals("重叠召回标签数", result.getRecallStage().get(0).get("recallFormulaLabel"));
        assertEquals("余弦相似度", result.getRankingStage().get(0).get("rankingFormulaLabel"));
        assertEquals(new BigDecimal("0.9100"), result.getRankingStage().get(0).get("interestScore"));
        assertEquals("兴趣分 + 场景分 + 可信分 × trustWeight", result.getRerankStage().get(0).get("finalScoreFormulaLabel"));
        assertTrue(result.getRerankStage().get(0).containsKey("trustBreakdown"));
        assertTrue(result.getRerankStage().get(0).containsKey("ruleDetails"));
        assertTrue(Boolean.TRUE.equals(result.getFinalStage().get(1).get("exploration")));
    }
}
