package com.lcj.campusreco.service.impl;

import com.lcj.campusreco.common.util.VectorUtils;
import com.lcj.campusreco.domain.model.ContributionItemModel;
import com.lcj.campusreco.domain.model.RankingCandidateModel;
import com.lcj.campusreco.domain.model.UserProfileModel;
import com.lcj.campusreco.mapper.TagMapper;
import com.lcj.campusreco.service.ProfileService;
import com.lcj.campusreco.service.RankingService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RankingServiceImpl implements RankingService {

    private final ProfileService profileService;
    private final TagMapper tagMapper;

    public RankingServiceImpl(ProfileService profileService, TagMapper tagMapper) {
        this.profileService = profileService;
        this.tagMapper = tagMapper;
    }

    @Override
    public List<RankingCandidateModel> rank(Long requestUserId, Set<Long> candidateUserIds) {
        UserProfileModel requestProfile = profileService.getProfile(requestUserId);
        Map<Long, String> tagNameMap = new HashMap<>();
        List<RankingCandidateModel> rankingList = new ArrayList<>();
        for (Long candidateUserId : candidateUserIds) {
            UserProfileModel candidateProfile = profileService.getProfile(candidateUserId);
            RankingCandidateModel candidateModel = new RankingCandidateModel();
            candidateModel.setTargetUserId(candidateUserId);
            candidateModel.setRecallScore(BigDecimal.valueOf(countOverlap(requestProfile, candidateProfile)));
            candidateModel.setContributions(buildContributions(requestProfile, candidateProfile, tagNameMap));
            candidateModel.getContributions().sort(Comparator.comparing(ContributionItemModel::getContributionScore).reversed());
            BigDecimal rankScore = VectorUtils.cosineSimilarity(requestProfile.getVector(), candidateProfile.getVector());
            candidateModel.setRankScore(rankScore);
            candidateModel.setRerankScore(BigDecimal.ZERO);
            candidateModel.setFinalScore(rankScore);
            rankingList.add(candidateModel);
        }
        rankingList.sort(Comparator.comparing(RankingCandidateModel::getRankScore, Comparator.nullsLast(BigDecimal::compareTo)).reversed());
        return rankingList;
    }

    private int countOverlap(UserProfileModel requestProfile, UserProfileModel candidateProfile) {
        int overlap = 0;
        for (Long tagId : requestProfile.getVector().keySet()) {
            if (candidateProfile.getVector().containsKey(tagId)) {
                overlap++;
            }
        }
        return overlap;
    }

    private List<ContributionItemModel> buildContributions(UserProfileModel requestProfile,
                                                           UserProfileModel candidateProfile,
                                                           Map<Long, String> tagNameMap) {
        List<ContributionItemModel> contributions = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> sourceEntry : requestProfile.getVector().entrySet()) {
            BigDecimal targetWeight = candidateProfile.getVector().get(sourceEntry.getKey());
            if (targetWeight == null) {
                continue;
            }
            ContributionItemModel item = new ContributionItemModel();
            item.setTagId(sourceEntry.getKey());
            item.setTagName(loadTagName(sourceEntry.getKey(), tagNameMap));
            item.setSourceWeight(sourceEntry.getValue());
            item.setTargetWeight(targetWeight);
            item.setContributionScore(sourceEntry.getValue().multiply(targetWeight));
            contributions.add(item);
        }
        return contributions;
    }

    private String loadTagName(Long tagId, Map<Long, String> tagNameMap) {
        return tagNameMap.computeIfAbsent(tagId, key -> {
            var entity = tagMapper.selectById(key);
            return entity == null ? "tag-" + key : entity.getTagName();
        });
    }
}
