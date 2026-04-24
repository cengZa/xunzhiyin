package com.lcj.campusreco.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcj.campusreco.common.constant.FeedbackType;
import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.entity.UserFeedbackEntity;
import com.lcj.campusreco.domain.entity.UserTagRelationEntity;
import com.lcj.campusreco.domain.model.TrustScoreResult;
import com.lcj.campusreco.mapper.UserFeedbackMapper;
import com.lcj.campusreco.mapper.UserTagRelationMapper;
import com.lcj.campusreco.service.TrustScoreService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TrustScoreServiceImpl implements TrustScoreService {

    private static final BigDecimal PROFILE_WEIGHT = new BigDecimal("0.4");
    private static final BigDecimal TAG_WEIGHT = new BigDecimal("0.3");
    private static final BigDecimal FOLLOW_WEIGHT = new BigDecimal("0.3");
    private static final BigDecimal PROFILE_STEP = new BigDecimal("0.1");
    private static final BigDecimal TAG_MAX_COUNT = new BigDecimal("4");
    private static final BigDecimal FOLLOW_MAX_COUNT = new BigDecimal("3");

    private final UserTagRelationMapper userTagRelationMapper;
    private final UserFeedbackMapper userFeedbackMapper;

    public TrustScoreServiceImpl(UserTagRelationMapper userTagRelationMapper,
                                 UserFeedbackMapper userFeedbackMapper) {
        this.userTagRelationMapper = userTagRelationMapper;
        this.userFeedbackMapper = userFeedbackMapper;
    }

    @Override
    public TrustScoreResult evaluate(UserEntity candidateUser) {
        if (candidateUser == null || candidateUser.getId() == null) {
            return new TrustScoreResult(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP), List.of());
        }

        BigDecimal profileScore = calculateProfileCompleteness(candidateUser);
        long tagCount = userTagRelationMapper.selectCount(
                new LambdaQueryWrapper<UserTagRelationEntity>()
                        .eq(UserTagRelationEntity::getUserId, candidateUser.getId())
        );
        BigDecimal tagScore = BigDecimal.valueOf(Math.min(tagCount, TAG_MAX_COUNT.longValue()))
                .divide(TAG_MAX_COUNT, 4, RoundingMode.HALF_UP)
                .multiply(TAG_WEIGHT);

        long followCount = userFeedbackMapper.selectList(
                        new LambdaQueryWrapper<UserFeedbackEntity>()
                                .eq(UserFeedbackEntity::getTargetUserId, candidateUser.getId())
                                .eq(UserFeedbackEntity::getFeedbackType, FeedbackType.FOLLOW)
                ).size();
        BigDecimal followScore = BigDecimal.valueOf(Math.min(followCount, FOLLOW_MAX_COUNT.longValue()))
                .divide(FOLLOW_MAX_COUNT, 4, RoundingMode.HALF_UP)
                .multiply(FOLLOW_WEIGHT);

        List<String> reasons = new ArrayList<>();
        if (profileScore.compareTo(new BigDecimal("0.3")) >= 0) {
            reasons.add("资料完整");
        }
        if (tagCount >= 3) {
            reasons.add("标签丰富");
        }
        if (followCount >= 2) {
            reasons.add("历史关注较多");
        }

        BigDecimal totalScore = profileScore.add(tagScore).add(followScore).setScale(4, RoundingMode.HALF_UP);
        return new TrustScoreResult(totalScore, reasons);
    }

    private BigDecimal calculateProfileCompleteness(UserEntity candidateUser) {
        BigDecimal score = BigDecimal.ZERO;
        if (hasText(candidateUser.getNickname())) {
            score = score.add(PROFILE_STEP);
        }
        if (hasText(candidateUser.getMajor())) {
            score = score.add(PROFILE_STEP);
        }
        if (hasText(candidateUser.getCollege())) {
            score = score.add(PROFILE_STEP);
        }
        if (candidateUser.getBio() != null && candidateUser.getBio().trim().length() >= 10) {
            score = score.add(PROFILE_STEP);
        }
        return score.min(PROFILE_WEIGHT).setScale(4, RoundingMode.HALF_UP);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
