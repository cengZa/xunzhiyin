package com.lcj.campusreco.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class UserProfileModel {

    private Long userId;
    private List<TagWeightModel> tagWeights = new ArrayList<>();
    private Map<Long, BigDecimal> vector = new HashMap<>();
    private List<TagWeightModel> topKTags = new ArrayList<>();
    private Integer profileVersion;
}
