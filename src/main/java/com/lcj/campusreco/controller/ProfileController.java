package com.lcj.campusreco.controller;

import com.lcj.campusreco.common.api.ApiResponse;
import com.lcj.campusreco.domain.model.UserProfileModel;
import com.lcj.campusreco.domain.model.TagWeightModel;
import com.lcj.campusreco.domain.vo.UserProfileVO;
import com.lcj.campusreco.service.ProfileService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping("/{userId}/build")
    public ApiResponse<UserProfileVO> buildProfile(@PathVariable Long userId) {
        return ApiResponse.success(toProfileVO(profileService.buildProfile(userId, "init")));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserProfileVO> getProfile(@PathVariable Long userId) {
        return ApiResponse.success(toProfileVO(profileService.getProfile(userId)));
    }

    private UserProfileVO toProfileVO(UserProfileModel profileModel) {
        UserProfileVO profileVO = new UserProfileVO();
        profileVO.setUserId(profileModel.getUserId());
        profileVO.setProfileVersion(profileModel.getProfileVersion());
        profileVO.setProfileJson(serializeVector(profileModel.getVector()));
        profileVO.setTopkJson(serializeTopK(profileModel.getTopKTags()));
        profileVO.setTopkTags(profileModel.getTopKTags().stream().map(TagWeightModel::getTagName).toList());
        profileVO.setUpdatedAt(LocalDateTime.now().toString());
        return profileVO;
    }

    private String serializeVector(Map<Long, java.math.BigDecimal> vector) {
        if (vector == null || vector.isEmpty()) {
            return "{}";
        }
        return vector.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .reduce((left, right) -> left + "," + right)
                .orElse("{}");
    }

    private String serializeTopK(List<TagWeightModel> topKTags) {
        if (topKTags == null || topKTags.isEmpty()) {
            return "[]";
        }
        return topKTags.stream()
                .map(tag -> tag.getTagId() + "|" + tag.getTagName() + "|" + tag.getFinalWeight())
                .reduce((left, right) -> left + "," + right)
                .orElse("[]");
    }
}
