package com.lcj.campusreco.service.impl;

import com.lcj.campusreco.common.constant.RecommendationScenarioMode;
import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.service.ExplorationService;
import com.lcj.campusreco.service.UserService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ExplorationServiceImpl implements ExplorationService {

    private static final BigDecimal MIN_TRUST_SCORE = new BigDecimal("0.1500");
    private static final BigDecimal CROSS_MAJOR_BONUS = new BigDecimal("0.1200");
    private static final BigDecimal SAME_MAJOR_BONUS = new BigDecimal("0.0400");
    private static final BigDecimal REASONABLE_INTEREST_SCORE = new BigDecimal("0.3500");

    private final UserService userService;

    public ExplorationServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public List<RankingCandidateModel> apply(Long requestUserId,
                                             List<RankingCandidateModel> rerankedList,
                                             int topK,
                                             String scenarioMode) {
        if (rerankedList == null || rerankedList.isEmpty() || topK <= 0) {
            return List.of();
        }
        List<RankingCandidateModel> cleanList = rerankedList.stream()
                .filter(Objects::nonNull)
                .toList();
        if (cleanList.isEmpty()) {
            return List.of();
        }

        resetExplorationFlags(cleanList);

        int limit = Math.min(topK, cleanList.size());
        if (!RecommendationScenarioMode.INTEREST_PARTNER.equals(RecommendationScenarioMode.normalize(scenarioMode))
                || topK < 3
                || cleanList.size() <= 3) {
            return new ArrayList<>(cleanList.subList(0, limit));
        }

        UserEntity requestUser = userService.getById(requestUserId);
        int stableCount = Math.min(2, limit);
        List<RankingCandidateModel> stable = new ArrayList<>(cleanList.subList(0, stableCount));

        RankingCandidateModel explorationCandidate = chooseExplorationCandidate(requestUser, cleanList, topK);
        List<RankingCandidateModel> result = new ArrayList<>(stable);
        Set<Long> chosenUserIds = new HashSet<>();
        stable.stream().map(RankingCandidateModel::getTargetUserId).forEach(chosenUserIds::add);

        if (explorationCandidate != null) {
            chosenUserIds.add(explorationCandidate.getTargetUserId());
        }

        int reservedSlots = explorationCandidate == null ? limit : limit - 1;
        for (RankingCandidateModel candidate : cleanList) {
            if (result.size() >= reservedSlots) {
                break;
            }
            if (!chosenUserIds.add(candidate.getTargetUserId())) {
                continue;
            }
            result.add(candidate);
        }

        if (explorationCandidate != null && result.size() < limit) {
            result.add(explorationCandidate);
        }

        if (result.size() > limit) {
            return new ArrayList<>(result.subList(0, limit));
        }
        return result;
    }

    private void resetExplorationFlags(List<RankingCandidateModel> candidates) {
        for (RankingCandidateModel candidate : candidates) {
            candidate.setExploration(false);
            candidate.setExplorationReason("");
            candidate.setExplorationScore(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        }
    }

    private RankingCandidateModel chooseExplorationCandidate(UserEntity requestUser,
                                                             List<RankingCandidateModel> candidates,
                                                             int topK) {
        List<RankingCandidateModel> explorationPool = new ArrayList<>();
        for (int index = Math.min(topK, candidates.size()); index < candidates.size(); index++) {
            RankingCandidateModel candidate = candidates.get(index);
            if (isEligible(candidate)) {
                explorationPool.add(candidate);
            }
        }
        if (explorationPool.isEmpty()) {
            return null;
        }

        return explorationPool.stream()
                .max(Comparator.comparing(candidate -> buildExplorationScore(requestUser, candidate)))
                .map(candidate -> markExplorationCandidate(requestUser, candidate))
                .orElse(null);
    }

    private boolean isEligible(RankingCandidateModel candidate) {
        BigDecimal trustScore = defaultScore(candidate.getTrustScore());
        BigDecimal interestScore = defaultScore(candidate.getInterestScore());
        return trustScore.compareTo(MIN_TRUST_SCORE) >= 0
                && interestScore.compareTo(REASONABLE_INTEREST_SCORE) >= 0;
    }

    private RankingCandidateModel markExplorationCandidate(UserEntity requestUser, RankingCandidateModel candidate) {
        BigDecimal explorationScore = buildExplorationScore(requestUser, candidate)
                .setScale(4, RoundingMode.HALF_UP);
        candidate.setExploration(true);
        candidate.setExplorationScore(explorationScore);
        candidate.setExplorationReason(buildExplorationReason(requestUser, candidate));
        return candidate;
    }

    private BigDecimal buildExplorationScore(UserEntity requestUser, RankingCandidateModel candidate) {
        BigDecimal interestScore = defaultScore(candidate.getInterestScore());
        BigDecimal trustScore = defaultScore(candidate.getTrustScore());
        BigDecimal campusScore = defaultScore(candidate.getCampusScore());
        BigDecimal scenarioBonus = isCrossMajor(requestUser, userService.getById(candidate.getTargetUserId()))
                ? CROSS_MAJOR_BONUS
                : SAME_MAJOR_BONUS;
        return interestScore
                .multiply(new BigDecimal("0.75"))
                .add(trustScore.multiply(new BigDecimal("0.35")))
                .subtract(campusScore.multiply(new BigDecimal("0.15")))
                .add(scenarioBonus);
    }

    private String buildExplorationReason(UserEntity requestUser, RankingCandidateModel candidate) {
        UserEntity candidateUser = userService.getById(candidate.getTargetUserId());
        boolean crossMajor = isCrossMajor(requestUser, candidateUser);
        if (crossMajor) {
            return "跨专业但兴趣标签高度重合，适合作为轻量探索位观察潜在连接";
        }
        if (defaultScore(candidate.getTrustScore()).compareTo(new BigDecimal("0.2500")) >= 0) {
            return "兴趣相近且资料可信度较高，保留为轻量探索位";
        }
        return "虽然校园场景加权较弱，但兴趣同频明显，适合作为探索候选";
    }

    private boolean isCrossMajor(UserEntity requestUser, UserEntity candidateUser) {
        if (requestUser == null || candidateUser == null) {
            return false;
        }
        String requestMajor = requestUser.getMajor();
        String candidateMajor = candidateUser.getMajor();
        return requestMajor != null
                && candidateMajor != null
                && !requestMajor.isBlank()
                && !candidateMajor.isBlank()
                && !requestMajor.equalsIgnoreCase(candidateMajor);
    }

    private BigDecimal defaultScore(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : value.setScale(4, RoundingMode.HALF_UP);
    }
}
