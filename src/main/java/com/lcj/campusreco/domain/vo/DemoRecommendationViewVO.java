package com.lcj.campusreco.domain.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class DemoRecommendationViewVO {

    private String viewCode;
    private String viewName;
    private String summary;
    private List<RecommendationItemVO> items = new ArrayList<>();
}
