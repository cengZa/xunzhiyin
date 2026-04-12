package com.lcj.campusreco.controller;

import com.lcj.campusreco.common.api.ApiResponse;
import com.lcj.campusreco.domain.entity.TagEntity;
import com.lcj.campusreco.service.TagService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ApiResponse<List<TagEntity>> listUserTags(@RequestParam Long userId) {
        return ApiResponse.success(tagService.listUserTags(userId));
    }
}
