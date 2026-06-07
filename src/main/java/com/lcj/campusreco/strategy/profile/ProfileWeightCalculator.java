package com.lcj.campusreco.strategy.profile;

import com.lcj.campusreco.domain.entity.TagEntity;
import com.lcj.campusreco.domain.entity.UserTagRelationEntity;
import com.lcj.campusreco.domain.model.TagWeightModel;
import java.util.Map;
import java.util.List;

public interface ProfileWeightCalculator {

    List<TagWeightModel> calculateWeights(List<UserTagRelationEntity> relations,
                                          List<TagEntity> tags,
                                          Map<Long, Long> tagDocumentFrequencies,
                                          long activeUserCount);
}
