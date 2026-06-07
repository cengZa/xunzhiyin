package com.lcj.campusreco.strategy.profile;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lcj.campusreco.domain.entity.TagEntity;
import com.lcj.campusreco.domain.entity.UserTagRelationEntity;
import com.lcj.campusreco.domain.model.TagWeightModel;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ImprovedTfIdfProfileWeightCalculatorTest {

    private final ImprovedTfIdfProfileWeightCalculator calculator = new ImprovedTfIdfProfileWeightCalculator();

    @Test
    void lowFrequencyTagReceivesHigherIdfThanPopularTag() {
        List<TagWeightModel> weights = calculator.calculateWeights(
                List.of(
                        relation(1L, 101L, 1, "1.0"),
                        relation(1L, 102L, 1, "1.0")
                ),
                List.of(tag(101L), tag(102L)),
                Map.of(101L, 8L, 102L, 1L),
                10
        );

        Map<Long, TagWeightModel> byTag = byTag(weights);

        assertTrue(byTag.get(102L).getIdf().compareTo(byTag.get(101L).getIdf()) > 0);
        assertTrue(byTag.get(102L).getFinalWeight().compareTo(byTag.get(101L).getFinalWeight()) > 0);
    }

    @Test
    void olderTagReceivesLowerTimeDecayAndFinalWeight() {
        List<TagWeightModel> weights = calculator.calculateWeights(
                List.of(
                        relation(1L, 101L, 2, "1.0"),
                        relation(1L, 102L, 90, "1.0")
                ),
                List.of(tag(101L), tag(102L)),
                Map.of(101L, 2L, 102L, 2L),
                10
        );

        Map<Long, TagWeightModel> byTag = byTag(weights);

        assertTrue(byTag.get(101L).getTimeDecay().compareTo(byTag.get(102L).getTimeDecay()) > 0);
        assertTrue(byTag.get(101L).getFinalWeight().compareTo(byTag.get(102L).getFinalWeight()) > 0);
    }

    @Test
    void strongerWeightSeedIncreasesFinalWeight() {
        List<TagWeightModel> weights = calculator.calculateWeights(
                List.of(
                        relation(1L, 101L, 3, "1.8"),
                        relation(1L, 102L, 3, "0.8")
                ),
                List.of(tag(101L), tag(102L)),
                Map.of(101L, 2L, 102L, 2L),
                10
        );

        Map<Long, TagWeightModel> byTag = byTag(weights);

        assertTrue(byTag.get(101L).getWeightSeed().compareTo(byTag.get(102L).getWeightSeed()) > 0);
        assertTrue(byTag.get(101L).getFinalWeight().compareTo(byTag.get(102L).getFinalWeight()) > 0);
    }

    private Map<Long, TagWeightModel> byTag(List<TagWeightModel> weights) {
        return weights.stream().collect(Collectors.toMap(TagWeightModel::getTagId, item -> item));
    }

    private TagEntity tag(Long tagId) {
        TagEntity tag = new TagEntity();
        tag.setId(tagId);
        tag.setTagName("tag-" + tagId);
        tag.setTagType("academic");
        return tag;
    }

    private UserTagRelationEntity relation(Long userId, Long tagId, int daysAgo, String weightSeed) {
        UserTagRelationEntity relation = new UserTagRelationEntity();
        relation.setUserId(userId);
        relation.setTagId(tagId);
        relation.setSelectedAt(LocalDateTime.now().minusDays(daysAgo));
        relation.setWeightSeed(new BigDecimal(weightSeed));
        return relation;
    }
}
