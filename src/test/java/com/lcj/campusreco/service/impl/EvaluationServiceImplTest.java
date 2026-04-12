package com.lcj.campusreco.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import org.mockito.InjectMocks;
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

    @InjectMocks
    private EvaluationServiceImpl evaluationService;

    @Test
    void generateSummaryComparesThreeBaselinesAndBuildsMarkdownReport() {
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
        when(rerankService.rerank(1L, List.of(candidateTwo, candidateThree))).thenReturn(List.of(candidateThree, candidateTwo));

        ExplanationVO explanationVO = new ExplanationVO();
        explanationVO.setReasonText("shared interests");
        when(explanationService.generate(any())).thenReturn(explanationVO);

        var summary = evaluationService.generateSummary(1);

        assertEquals(1, summary.getActiveUserCount());
        assertEquals(3, summary.getTagCount());
        assertEquals(4, summary.getRelationCount());
        assertEquals(3, summary.getBaselines().size());

        EvaluationBaselineVO overlap = summary.getBaselines().stream()
                .filter(item -> "tag_overlap".equals(item.getBaselineCode()))
                .findFirst()
                .orElseThrow();
        EvaluationBaselineVO cosine = summary.getBaselines().stream()
                .filter(item -> "cosine_similarity".equals(item.getBaselineCode()))
                .findFirst()
                .orElseThrow();
        EvaluationBaselineVO full = summary.getBaselines().stream()
                .filter(item -> "full_pipeline".equals(item.getBaselineCode()))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("2.0000"), overlap.getAverageRecallCandidateCount());
        assertEquals(new BigDecimal("0.0000"), overlap.getPrecisionAtK());
        assertEquals(new BigDecimal("1.0000"), cosine.getPrecisionAtK());
        assertEquals(new BigDecimal("1.0000"), full.getHitRateAtK());
        assertEquals(new BigDecimal("1.0000"), full.getExplanationPresenceRate());

        String markdown = evaluationService.generateMarkdownReport(1);
        assertTrue(markdown.contains("| Baseline |"));
        assertTrue(markdown.contains("Tag Overlap"));
        assertTrue(markdown.contains("Full Pipeline"));
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
        candidate.setRerankScore(new BigDecimal(rerankScore));
        candidate.setFinalScore(new BigDecimal(finalScore));
        return candidate;
    }
}
