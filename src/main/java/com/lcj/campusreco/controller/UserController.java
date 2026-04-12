package com.lcj.campusreco.controller;

import com.lcj.campusreco.common.api.ApiResponse;
import com.lcj.campusreco.domain.dto.UserCreateDTO;
import com.lcj.campusreco.domain.dto.UserTagBindDTO;
import com.lcj.campusreco.domain.entity.TagEntity;
import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.vo.UserVO;
import com.lcj.campusreco.service.TagService;
import com.lcj.campusreco.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final TagService tagService;

    public UserController(UserService userService, TagService tagService) {
        this.userService = userService;
        this.tagService = tagService;
    }

    @PostMapping
    public ApiResponse<Map<String, Long>> createUser(@Valid @RequestBody UserCreateDTO dto) {
        return ApiResponse.success(Map.of("userId", userService.createUser(dto)));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserVO> getUser(@PathVariable Long userId) {
        UserEntity entity = userService.getById(userId);
        List<TagEntity> tags = tagService.listUserTags(userId);
        UserVO userVO = new UserVO();
        if (entity != null) {
            userVO.setUserId(entity.getId());
            userVO.setStudentNo(entity.getStudentNo());
            userVO.setNickname(entity.getNickname());
            userVO.setGender(entity.getGender());
            userVO.setGrade(entity.getGrade());
            userVO.setMajor(entity.getMajor());
            userVO.setCollege(entity.getCollege());
            userVO.setBio(entity.getBio());
        }
        userVO.setTags(tags.stream().map(TagEntity::getTagName).toList());
        return ApiResponse.success(userVO);
    }

    @PostMapping("/{userId}/tags")
    public ApiResponse<Void> bindUserTags(@PathVariable Long userId, @Valid @RequestBody UserTagBindDTO dto) {
        tagService.bindUserTags(userId, dto.getTagIds(), dto.getSourceType());
        return ApiResponse.success(null);
    }

    @GetMapping("/{userId}/tags")
    public ApiResponse<List<TagEntity>> listUserTags(@PathVariable Long userId) {
        return ApiResponse.success(tagService.listUserTags(userId));
    }
}
