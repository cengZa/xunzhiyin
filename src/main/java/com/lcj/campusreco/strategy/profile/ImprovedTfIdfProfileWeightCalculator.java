package com.lcj.campusreco.strategy.profile;

import com.lcj.campusreco.domain.entity.TagEntity;
import com.lcj.campusreco.domain.entity.UserTagRelationEntity;
import com.lcj.campusreco.domain.model.TagWeightModel;
import com.lcj.campusreco.common.util.TimeDecayUtils;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ImprovedTfIdfProfileWeightCalculator implements ProfileWeightCalculator {

    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
    private static final BigDecimal DECAY_FACTOR = BigDecimal.valueOf(0.02D);

    @Override
    public List<TagWeightModel> calculateWeights(List<UserTagRelationEntity> relations, List<TagEntity> tags) {
        if (relations == null || relations.isEmpty() || tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, TagEntity> tagMap = new HashMap<>();
        for (TagEntity tag : tags) {
            tagMap.put(tag.getId(), tag);
        }
        BigDecimal relationCount = BigDecimal.valueOf(relations.size());
        Map<Long, TagWeightModel> aggregated = new HashMap<>();
        for (UserTagRelationEntity relation : relations) {
            TagEntity tagEntity = tagMap.get(relation.getTagId());
            if (tagEntity == null) {
                continue;
            }
            BigDecimal seed = relation.getWeightSeed() == null ? BigDecimal.ONE : relation.getWeightSeed();
            BigDecimal tf = BigDecimal.ONE.divide(relationCount, MATH_CONTEXT);
            BigDecimal idf = BigDecimal.ONE;
            BigDecimal timeDecay = TimeDecayUtils.calculateDecay(
                    relation.getSelectedAt(),
                    LocalDateTime.now(),
                    DECAY_FACTOR
            );

            TagWeightModel tagWeightModel = aggregated.computeIfAbsent(tagEntity.getId(), key -> {
                TagWeightModel model = new TagWeightModel();
                model.setTagId(tagEntity.getId());
                model.setTagName(tagEntity.getTagName());
                model.setTagType(tagEntity.getTagType());
                model.setTf(BigDecimal.ZERO);
                model.setIdf(idf);
                model.setTimeDecay(timeDecay);
                model.setFinalWeight(BigDecimal.ZERO);
                return model;
            });
            tagWeightModel.setTf(tagWeightModel.getTf().add(tf, MATH_CONTEXT));
            tagWeightModel.setTimeDecay(timeDecay);
            tagWeightModel.setFinalWeight(tagWeightModel.getFinalWeight()
                    .add(tf.multiply(idf, MATH_CONTEXT).multiply(timeDecay, MATH_CONTEXT).multiply(seed, MATH_CONTEXT), MATH_CONTEXT));
        }
        return new ArrayList<>(aggregated.values());
    }
}
