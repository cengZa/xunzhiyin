package com.lcj.campusreco.domain.vo;

import java.util.List;
import lombok.Data;

@Data
public class UserProfileVO {

    private Long userId;
    private Integer profileVersion;
    private String profileJson;
    private String topkJson;
    private List<String> topkTags;
    private String updatedAt;
}
