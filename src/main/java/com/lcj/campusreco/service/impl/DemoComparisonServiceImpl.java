package com.lcj.campusreco.service.impl;

import com.lcj.campusreco.common.constant.RecommendationScenarioMode;
import com.lcj.campusreco.config.RecommendationTuningContext;
import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.UserProfileModel;
import com.lcj.campusreco.domain.vo.DemoComparisonVO;
import com.lcj.campusreco.domain.vo.DemoRecommendationViewVO;
import com.lcj.campusreco.domain.vo.ExplanationVO;
import com.lcj.campusreco.domain.vo.RecommendationItemVO;
import com.lcj.campusreco.service.DemoComparisonService;
import com.lcj.campusreco.service.ExplorationService;
import com.lcj.campusreco.service.ExplanationService;
import com.lcj.campusreco.service.ProfileService;
import com.lcj.campusreco.service.RankingService;
import com.lcj.campusreco.service.RecallService;
import com.lcj.campusreco.service.RerankService;
import com.lcj.campusreco.service.UserService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DemoComparisonServiceImpl implements DemoComparisonService {

    private final ProfileService profileService;
    private final RecallService recallService;
    private final RankingService rankingService;
    private final RerankService rerankService;
    private final ExplorationService explorationService;
    private final ExplanationService explanationService;
    private final UserService userService;
    private final RecommendationTuningContext tuningContext;

    public DemoComparisonServiceImpl(ProfileService profileService,
                                     RecallService recallService,
                                     RankingService rankingService,
                                     RerankService rerankService,
                                     ExplorationService explorationService,
                                     ExplanationService explanationService,
                                     UserService userService,
                                     RecommendationTuningContext tuningContext) {
        this.profileService = profileService;
        this.recallService = recallService;
        this.rankingService = rankingService;
        this.rerankService = rerankService;
        this.explorationService = explorationService;
        this.explanationService = explanationService;
        this.userService = userService;
        this.tuningContext = tuningContext;
    }

    @Override
    public DemoComparisonVO compareViews(Long userId, Integer topK, String scenarioMode) {
        int effectiveTopK = topK == null || topK < 1 ? 3 : topK;
        String normalizedMode = RecommendationScenarioMode.normalize(scenarioMode);
        UserProfileModel profileModel = profileService.getProfile(userId);
        if (profileModel.getVector().isEmpty()) {
            profileModel = profileService.buildProfile(userId, "demo_compare");
        }

        var candidateUserIds = recallService.recallCandidateUserIds(profileModel);
        List<RankingCandidateModel> rankingList = rankingService.rank(userId, candidateUserIds);
        List<RankingCandidateModel> tagOverlapList = cloneCandidates(rankingList).stream()
                .sorted(Comparator.comparing(RankingCandidateModel::getRecallScore, Comparator.nullsLast(java.math.BigDecimal::compareTo))
                        .reversed()
                        .thenComparing(Comparator.comparing(
                                RankingCandidateModel::getRankScore,
                                Comparator.nullsLast(java.math.BigDecimal::compareTo)
                        ).reversed()))
                .limit(effectiveTopK)
                .toList();
        List<RankingCandidateModel> fullPipelineList;
        try (RecommendationTuningContext.Scope ignored = tuningContext.withOverrides(null, null, normalizedMode, true)) {
            List<RankingCandidateModel> rerankedList = rerankService.rerank(userId, cloneCandidates(rankingList));
            fullPipelineList = explorationService.apply(userId, rerankedList, effectiveTopK, normalizedMode);
        }

        DemoComparisonVO comparison = new DemoComparisonVO();
        comparison.setUserId(userId);
        comparison.setTopK(effectiveTopK);
        comparison.setCandidateCount(candidateUserIds.size());
        comparison.setScenarioMode(normalizedMode);
        comparison.setScenarioLabel(RecommendationScenarioMode.labelOf(normalizedMode));
        comparison.setTagOverlapView(buildView(
                "tag_overlap",
                "标签重叠基线",
                "仅按共同兴趣标签和召回强度排序，适合用来对比新算法的改进幅度。",
                tagOverlapList,
                RecommendationScenarioMode.labelOf(normalizedMode)
        ));
        comparison.setFullPipelineView(buildView(
                "full_pipeline",
                "完整链路",
                    "在兴趣相似度基础上叠加当前场景重排和可信连接分，更适合演示展示“新算法匹配”。",
                fullPipelineList,
                RecommendationScenarioMode.labelOf(normalizedMode)
        ));
        return comparison;
    }

    private DemoRecommendationViewVO buildView(String viewCode,
                                               String viewName,
                                               String summary,
                                               List<RankingCandidateModel> candidates,
                                               String scenarioLabel) {
        DemoRecommendationViewVO view = new DemoRecommendationViewVO();
        view.setViewCode(viewCode);
        view.setViewName(viewName);
        view.setSummary(summary);
        Map<Long, UserEntity> userCache = new HashMap<>();
        int rankNo = 1;
        for (RankingCandidateModel candidate : candidates) {
            ExplanationVO explanation = explanationService.generate(candidate);
            RecommendationItemVO item = new RecommendationItemVO();
            item.setTargetUserId(candidate.getTargetUserId());
            item.setTargetNickname(resolveNickname(candidate.getTargetUserId(), userCache));
            item.setRecallScore(candidate.getRecallScore());
            item.setRankScore(candidate.getRankScore());
            item.setInterestScore(candidate.getInterestScore() == null ? candidate.getRankScore() : candidate.getInterestScore());
            item.setRerankScore(candidate.getRerankScore());
            item.setCampusScore(candidate.getCampusScore() == null ? candidate.getRerankScore() : candidate.getCampusScore());
            item.setTrustScore(candidate.getTrustScore());
            item.setFinalScore(candidate.getFinalScore());
            item.setRankNo(rankNo++);
            item.setScenarioMode(candidate.getScenarioMode() == null ? RecommendationScenarioMode.INTEREST_PARTNER : candidate.getScenarioMode());
            item.setScenarioLabel(candidate.getScenarioLabel() == null ? scenarioLabel : candidate.getScenarioLabel());
            item.getMatchedTags().addAll(candidate.getContributions().stream()
                    .map(contribution -> contribution.getTagName())
                    .filter(tagName -> tagName != null && !tagName.isBlank())
                    .limit(3)
                    .toList());
            item.getMatchedRules().addAll(candidate.getRuleHits().stream()
                    .filter(ruleHit -> Boolean.TRUE.equals(ruleHit.getHit()))
                    .map(ruleHit -> ruleHit.getRuleDesc())
                    .filter(ruleDesc -> ruleDesc != null && !ruleDesc.isBlank())
                    .toList());
            item.getTrustReasons().addAll(candidate.getTrustReasons());
            item.setExploration(candidate.isExploration());
            item.setExplorationScore(candidate.getExplorationScore());
            item.setExplorationReason(candidate.getExplorationReason());
            item.setRecommendationLabel(RecommendationScenarioMode.recommendationLabelOf(
                    item.getScenarioMode(),
                    !item.getMatchedRules().isEmpty()
            ));
            if (explanation != null) {
                item.setExplanation(explanation.getReasonText());
                item.setReasonText(explanation.getReasonText());
            }
            view.getItems().add(item);
        }
        return view;
    }

    private String resolveNickname(Long targetUserId, Map<Long, UserEntity> userCache) {
        UserEntity targetUser = userCache.computeIfAbsent(targetUserId, userService::getById);
        if (targetUser == null || targetUser.getNickname() == null || targetUser.getNickname().isBlank()) {
            return "用户-" + targetUserId;
        }
        return targetUser.getNickname();
    }

    private List<RankingCandidateModel> cloneCandidates(List<RankingCandidateModel> rankingList) {
        List<RankingCandidateModel> copies = new ArrayList<>();
        for (RankingCandidateModel candidate : rankingList) {
            RankingCandidateModel copy = new RankingCandidateModel();
            copy.setTargetUserId(candidate.getTargetUserId());
            copy.setRecallScore(candidate.getRecallScore());
            copy.setRankScore(candidate.getRankScore());
            copy.setInterestScore(candidate.getInterestScore());
            copy.setRerankScore(candidate.getRerankScore());
            copy.setCampusScore(candidate.getCampusScore());
            copy.setTrustScore(candidate.getTrustScore());
            copy.setFinalScore(candidate.getFinalScore());
            copy.setScenarioMode(candidate.getScenarioMode());
            copy.setScenarioLabel(candidate.getScenarioLabel());
            copy.setExploration(candidate.isExploration());
            copy.setExplorationScore(candidate.getExplorationScore());
            copy.setExplorationReason(candidate.getExplorationReason());
            copy.setTrustReasons(new ArrayList<>(candidate.getTrustReasons()));
            copy.setContributions(new ArrayList<>(candidate.getContributions()));
            copy.setRuleHits(new ArrayList<>(candidate.getRuleHits()));
            copies.add(copy);
        }
        return copies;
    }
}
