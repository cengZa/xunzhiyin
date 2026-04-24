package com.lcj.campusreco.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcj.campusreco.domain.entity.UserEntity;
import com.lcj.campusreco.domain.entity.UserFeedbackEntity;
import com.lcj.campusreco.domain.entity.UserTagRelationEntity;
import com.lcj.campusreco.domain.model.TrustScoreResult;
import com.lcj.campusreco.mapper.UserFeedbackMapper;
import com.lcj.campusreco.mapper.UserTagRelationMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrustScoreServiceImplTest {

    @Mock
    private UserTagRelationMapper userTagRelationMapper;

    @Mock
    private UserFeedbackMapper userFeedbackMapper;

    @Test
    void evaluateReturnsFullScoreForRichAndTrustedCandidate() {
        TrustScoreServiceImpl service = new TrustScoreServiceImpl(userTagRelationMapper, userFeedbackMapper);

        UserEntity candidate = new UserEntity();
        candidate.setId(2002L);
        candidate.setNickname("柏宇");
        candidate.setMajor("Computer Science");
        candidate.setCollege("Engineering");
        candidate.setBio("长期参与 ACM 和 Java 技术分享活动");

        when(userTagRelationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(4L);
        when(userFeedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                createFeedback(), createFeedback(), createFeedback()
        ));

        TrustScoreResult result = service.evaluate(candidate);

        assertEquals(new BigDecimal("1.0000"), result.score());
        assertEquals(List.of("资料完整", "标签丰富", "历史关注较多"), result.reasons());
    }

    @Test
    void evaluateReturnsLowerScoreForSparseCandidate() {
        TrustScoreServiceImpl service = new TrustScoreServiceImpl(userTagRelationMapper, userFeedbackMapper);

        UserEntity candidate = new UserEntity();
        candidate.setId(2003L);
        candidate.setNickname("阿北");

        when(userTagRelationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(userFeedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        TrustScoreResult result = service.evaluate(candidate);

        assertEquals(new BigDecimal("0.1750"), result.score());
        assertTrue(result.reasons().isEmpty());
    }

    private UserFeedbackEntity createFeedback() {
        UserFeedbackEntity entity = new UserFeedbackEntity();
        entity.setFeedbackType("follow");
        entity.setFeedbackTime(LocalDateTime.now());
        return entity;
    }
}
