package com.lcj.campusreco.common.constant;

import java.util.List;

public final class RecommendationScenarioMode {

    public static final String STUDY_PARTNER = "study_partner";
    public static final String CLUB_PARTNER = "club_partner";
    public static final String INTEREST_PARTNER = "interest_partner";

    private RecommendationScenarioMode() {
    }

    public static String normalize(String scenarioMode) {
        if (scenarioMode == null || scenarioMode.isBlank()) {
            return INTEREST_PARTNER;
        }
        return switch (scenarioMode.trim().toLowerCase()) {
            case STUDY_PARTNER -> STUDY_PARTNER;
            case CLUB_PARTNER -> CLUB_PARTNER;
            default -> INTEREST_PARTNER;
        };
    }

    public static String labelOf(String scenarioMode) {
        return switch (normalize(scenarioMode)) {
            case STUDY_PARTNER -> "学习搭子";
            case CLUB_PARTNER -> "社团搭子";
            default -> "兴趣搭子";
        };
    }

    public static String recommendationLabelOf(String scenarioMode, boolean hasCampusSignal) {
        return switch (normalize(scenarioMode)) {
            case STUDY_PARTNER -> hasCampusSignal ? "学习搭子匹配：兴趣相似 + 学习场景加权" : "学习搭子匹配";
            case CLUB_PARTNER -> hasCampusSignal ? "社团活动匹配：兴趣相似 + 校园活动加权" : "社团活动匹配";
            default -> hasCampusSignal ? "兴趣同频匹配：兴趣相似 + 校园场景加权" : "兴趣同频匹配";
        };
    }

    public static List<String> allModes() {
        return List.of(CLUB_PARTNER, INTEREST_PARTNER, STUDY_PARTNER);
    }
}
