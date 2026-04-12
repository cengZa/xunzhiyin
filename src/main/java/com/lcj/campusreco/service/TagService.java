package com.lcj.campusreco.service;

import com.lcj.campusreco.domain.entity.TagEntity;
import java.util.List;

public interface TagService {

    void bindUserTags(Long userId, List<Long> tagIds, String sourceType);

    List<TagEntity> listUserTags(Long userId);
}
