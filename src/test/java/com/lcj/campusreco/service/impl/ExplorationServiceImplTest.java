package com.lcj.campusreco.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.service.UserService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExplorationServiceImplTest {

    @Mock
    private UserService userService;

    @Test
    void applyKeepsTopTwoStableAndUsesLastSlotForExploration() {
        ExplorationServiceImpl explorationService = new ExplorationServiceImpl(userService);

        UserEntity requestUser = createUser(2001L, "计算机科学");
        UserEntity crossMajor = createUser(2004L, "数字媒体技术");

        when(userService.getById(2001L)).thenReturn(requestUser);
        when(userService.getById(2004L)).thenReturn(crossMajor);

        RankingCandidateModel first = candidate(2002L, "0.9300", "0.1200", "0.2800", "1.0920");
        RankingCandidateModel second = candidate(2003L, "0.8900", "0.0800", "0.2400", "1.0060");
        RankingCandidateModel exploration = candidate(2004L, "0.8500", "0.0200", "0.2200", "0.9030");
        RankingCandidateModel rejected = candidate(2005L, "0.7900", "0.0100", "0.0400", "0.8060");

        List<RankingCandidateModel> result = explorationService.apply(
                2001L,
                List.of(first, second, rejected, exploration),
                3,
                "interest_partner"
        );

        assertEquals(3, result.size());
        assertEquals(2002L, result.get(0).getTargetUserId());
        assertEquals(2003L, result.get(1).getTargetUserId());
        assertEquals(2004L, result.get(2).getTargetUserId());
        assertTrue(result.get(2).isExploration());
        assertTrue(result.get(2).getExplorationScore().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.get(2).getExplorationReason().contains("跨专业"));
        assertFalse(result.get(0).isExploration());
        assertFalse(result.get(1).isExploration());
    }

    @Test
    void applyDoesNotEnableExplorationForStudyPartner() {
        ExplorationServiceImpl explorationService = new ExplorationServiceImpl(userService);
        List<RankingCandidateModel> ordered = List.of(
                candidate(2002L, "0.9300", "0.1200", "0.2800", "1.0920"),
                candidate(2003L, "0.8900", "0.0800", "0.2400", "1.0060"),
                candidate(2004L, "0.8500", "0.0200", "0.2200", "0.9030")
        );

        List<RankingCandidateModel> result = explorationService.apply(2001L, ordered, 3, "study_partner");

        assertEquals(3, result.size());
        assertEquals(2002L, result.get(0).getTargetUserId());
        assertEquals(2003L, result.get(1).getTargetUserId());
        assertEquals(2004L, result.get(2).getTargetUserId());
        assertFalse(result.get(2).isExploration());
    }

    private RankingCandidateModel candidate(Long userId,
                                            String interestScore,
                                            String campusScore,
                                            String trustScore,
                                            String finalScore) {
        RankingCandidateModel candidate = new RankingCandidateModel();
        candidate.setTargetUserId(userId);
        candidate.setRankScore(new BigDecimal(interestScore));
        candidate.setInterestScore(new BigDecimal(interestScore));
        candidate.setCampusScore(new BigDecimal(campusScore));
        candidate.setRerankScore(new BigDecimal(campusScore));
        candidate.setTrustScore(new BigDecimal(trustScore));
        candidate.setFinalScore(new BigDecimal(finalScore));
        return candidate;
    }

    private UserEntity createUser(Long userId, String major) {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setMajor(major);
        return userEntity;
    }
}
