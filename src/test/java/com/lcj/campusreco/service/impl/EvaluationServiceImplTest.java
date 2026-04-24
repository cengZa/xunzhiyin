package com.lcj.campusreco.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.lcj.campusreco.config.RecommendationTuningContext;
import com.lcj.campusreco.domain.entity.TagEntity;
import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.entity.UserTagRelationEntity;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.UserProfileModel;
import com.lcj.campusreco.domain.vo.EvaluationBaselineVO;
import com.lcj.campusreco.domain.vo.ExplanationVO;
import com.lcj.campusreco.mapper.TagMapper;
import com.lcj.campusreco.mapper.UserMapper;
import com.lcj.campusreco.mapper.UserTagRelationMapper;
import com.lcj.campusreco.service.ExplanationService;
import com.lcj.campusreco.service.ProfileService;
import com.lcj.campusreco.service.RankingService;
import com.lcj.campusreco.service.RecallService;
import com.lcj.campusreco.service.RerankService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private TagMapper tagMapper;
    @Mock
    private UserTagRelationMapper userTagRelationMapper;
    @Mock
    private ProfileService profileService;
    @Mock
    private RecallService recallService;
    @Mock
    private RankingService rankingService;
    @Mock
    private RerankService rerankService;
    @Mock
    private ExplanationService explanationService;

    @Test
    void generateSummaryBuildsScenarioAwareBaselines() {
        EvaluationServiceImpl evaluationService = new EvaluationServiceImpl(
                userMapper,
                tagMapper,
                userTagRelationMapper,
                profileService,
                recallService,
                rankingService,
                rerankService,
                explanationService,
                new RecommendationTuningContext(5, BigDecimal.ONE, "study_partner", true)
        );

        UserEntity requestUser = createUser(1L, "Computer Science");
        UserEntity nonRelevantCandidate = createUser(2L, "Mathematics");
        UserEntity relevantCandidate = createUser(3L, "Computer Science");

        when(userMapper.selectList(any())).thenReturn(List.of(requestUser));
        when(userMapper.selectById(2L)).thenReturn(nonRelevantCandidate);
        when(userMapper.selectById(3L)).thenReturn(relevantCandidate);
        when(tagMapper.selectList(any())).thenReturn(List.of(createTag(101L), createTag(102L), createTag(103L)));
        when(userTagRelationMapper.selectList(any())).thenReturn(List.of(
                createRelation(1L, 101L),
                createRelation(1L, 102L),
                createRelation(2L, 101L),
                createRelation(3L, 102L)
        ));

        UserProfileModel profileModel = new UserProfileModel();
        profileModel.setUserId(1L);
        profileModel.getVector().put(101L, BigDecimal.ONE);
        profileModel.getVector().put(102L, BigDecimal.ONE);
        when(profileService.getProfile(1L)).thenReturn(profileModel);
        when(recallService.recallCandidateUserIds(profileModel)).thenReturn(Set.of(2L, 3L));

        RankingCandidateModel candidateTwo = createCandidate(2L, "1.0", "0.2", "0.0", "0.2");
        RankingCandidateModel candidateThree = createCandidate(3L, "1.0", "0.9", "0.1", "1.0");
        when(rankingService.rank(1L, Set.of(2L, 3L))).thenReturn(List.of(candidateTwo, candidateThree));
        when(rerankService.rerank(eq(1L), any()))
                .thenReturn(List.of(candidateThree, candidateTwo));

        ExplanationVO explanationVO = new ExplanationVO();
        explanationVO.setReasonText("shared interests");
        when(explanationService.generate(any())).thenReturn(explanationVO);

        var summary = evaluationService.generateSummary(1);

        assertEquals("study_partner", summary.getScenarioMode());
        assertEquals("学习搭子", summary.getScenarioLabel());
        assertEquals(1, summary.getActiveUserCount());
        assertEquals(3, summary.getTagCount());
        assertEquals(4, summary.getRelationCount());
        assertEquals(4, summary.getBaselines().size());

        EvaluationBaselineVO overlap = find(summary, "tag_overlap");
        EvaluationBaselineVO cosine = find(summary, "cosine_similarity");
        EvaluationBaselineVO fullNoTrust = find(summary, "full_pipeline_no_trust");
        EvaluationBaselineVO fullWithTrust = find(summary, "full_pipeline_with_trust");

        assertEquals(new BigDecimal("2.0000"), overlap.getAverageRecallCandidateCount());
        assertEquals(new BigDecimal("0.0000"), overlap.getPrecisionAtK());
        assertEquals(new BigDecimal("1.0000"), cosine.getPrecisionAtK());
        assertEquals(new BigDecimal("1.0000"), fullNoTrust.getHitRateAtK());
        assertEquals(new BigDecimal("1.0000"), fullWithTrust.getExplanationPresenceRate());

        String markdown = evaluationService.generateMarkdownReport(1);
        assertTrue(markdown.contains("| 基线 |"));
        assertTrue(markdown.contains("标签重叠"));
        assertTrue(markdown.contains("完整链路（含可信分）"));
    }

    private EvaluationBaselineVO find(com.lcj.campusreco.domain.vo.EvaluationSummaryVO summary, String code) {
        return summary.getBaselines().stream()
                .filter(item -> code.equals(item.getBaselineCode()))
                .findFirst()
                .orElseThrow();
    }

    private UserEntity createUser(Long id, String major) {
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setNickname("user-" + id);
        entity.setMajor(major);
        entity.setStatus(1);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    private TagEntity createTag(Long id) {
        TagEntity entity = new TagEntity();
        entity.setId(id);
        entity.setTagName("tag-" + id);
        entity.setStatus(1);
        return entity;
    }

    private UserTagRelationEntity createRelation(Long userId, Long tagId) {
        UserTagRelationEntity relation = new UserTagRelationEntity();
        relation.setUserId(userId);
        relation.setTagId(tagId);
        relation.setSelectedAt(LocalDateTime.now());
        return relation;
    }

    private RankingCandidateModel createCandidate(Long targetUserId,
                                                  String recallScore,
                                                  String rankScore,
                                                  String rerankScore,
                                                  String finalScore) {
        RankingCandidateModel candidate = new RankingCandidateModel();
        candidate.setTargetUserId(targetUserId);
        candidate.setRecallScore(new BigDecimal(recallScore));
        candidate.setRankScore(new BigDecimal(rankScore));
        candidate.setInterestScore(new BigDecimal(rankScore));
        candidate.setRerankScore(new BigDecimal(rerankScore));
        candidate.setCampusScore(new BigDecimal(rerankScore));
        candidate.setFinalScore(new BigDecimal(finalScore));
        return candidate;
    }
}
