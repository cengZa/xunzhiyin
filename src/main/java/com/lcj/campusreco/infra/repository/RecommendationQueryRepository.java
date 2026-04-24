package com.lcj.campusreco.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcj.campusreco.domain.entity.RecommendationExplanationEntity;
import com.lcj.campusreco.domain.entity.RecommendationResultEntity;
import com.lcj.campusreco.mapper.RecommendationExplanationMapper;
import com.lcj.campusreco.mapper.RecommendationResultMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class RecommendationQueryRepository {

    private final RecommendationResultMapper recommendationResultMapper;
    private final RecommendationExplanationMapper recommendationExplanationMapper;

    public RecommendationQueryRepository(RecommendationResultMapper recommendationResultMapper,
                                         RecommendationExplanationMapper recommendationExplanationMapper) {
        this.recommendationResultMapper = recommendationResultMapper;
        this.recommendationExplanationMapper = recommendationExplanationMapper;
    }

    public List<RecommendationResultEntity> listByRequestUserId(Long userId) {
        return recommendationResultMapper.selectList(
                new LambdaQueryWrapper<RecommendationResultEntity>()
                        .eq(RecommendationResultEntity::getRequestUserId, userId)
                        .orderByAsc(RecommendationResultEntity::getRankNo)
        );
    }

    public RecommendationResultEntity getRecommendationResultById(Long recommendationId) {
        return recommendationResultMapper.selectById(recommendationId);
    }

    public RecommendationExplanationEntity getExplanationByRecommendationId(Long recommendationId) {
        return recommendationExplanationMapper.selectOne(
                new LambdaQueryWrapper<RecommendationExplanationEntity>()
                        .eq(RecommendationExplanationEntity::getRecommendationId, recommendationId)
                        .last("limit 1")
        );
    }
}
