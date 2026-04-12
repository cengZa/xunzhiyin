# Service Design
# Service 设计

## 1. UserService
职责：
- 用户创建
- 用户查询
- 用户基础信息读取

核心方法：
- Long createUser(UserCreateDTO dto)
- UserEntity getById(Long userId)
- List<UserEntity> listByIds(List<Long> userIds)

## 2. TagService
职责：
- 标签定义管理
- 用户标签设置与查询

核心方法：
- void bindUserTags(Long userId, List<Long> tagIds, String sourceType)
- List<TagEntity> listUserTags(Long userId)

## 3. ProfileService
职责：
- 计算用户画像
- 读取用户画像
- 反馈后重建画像

核心方法：
- UserProfileModel buildProfile(Long userId, String updatedBy)
- UserProfileModel getProfile(Long userId)
- void rebuildProfile(Long userId, String updatedBy)

## 4. RecallService
职责：
- 根据目标用户 Top-K 标签召回候选用户

核心方法：
- Set<Long> recallCandidateUserIds(UserProfileModel profile)

实现说明：
- 读取 Redis 中 recall:inv:tag:{tagId}
- 合并候选集合
- 去掉自己
- 返回候选用户ID集合

## 5. RankingService
职责：
- 对候选用户计算余弦相似度
- 记录标签贡献项

核心方法：
- List<RankingCandidateModel> rank(Long requestUserId, Set<Long> candidateUserIds)

实现说明：
- 获取目标用户画像
- 批量获取候选用户画像
- 逐个计算余弦相似度
- 构建 contributions

## 6. RerankService
职责：
- 根据校园规则调整排序结果

核心方法：
- List<RankingCandidateModel> rerank(Long requestUserId, List<RankingCandidateModel> rankingList)

规则建议：
- 年级差 <= 1：加分
- 专业相同或相近：加分
- 社团重合：加分
- 可选轻度多样性：去过度同质化

## 7. ExplanationService
职责：
- 生成推荐解释
- 保证解释与排序/重排逻辑一致

核心方法：
- ExplanationVO generate(RankingCandidateModel candidate)
- void batchSaveExplanation(List<RankingCandidateModel> candidates, Map<Long, Long> recommendationIdMap)

## 8. FeedbackService
职责：
- 提交反馈
- 触发轻量画像更新

核心方法：
- void submitFeedback(Long requestUserId, FeedbackSubmitDTO dto)
- void applyFeedbackUpdate(Long requestUserId, Long recommendationId, String feedbackType)

实现说明：
- follow：提高相关贡献标签权重
- ignore：轻度降低相关贡献标签权重
- 更新画像并刷新缓存

## 9. RecommendationService
职责：
- 编排推荐主链路

核心方法：
- RecommendationDetailVO recommend(RecommendRequestDTO dto)

主流程：
1. 获取目标用户画像
2. 召回候选集合
3. 排序
4. 重排
5. 截取 Top-K
6. 保存 recommendation_result
7. 生成并保存 explanation
8. 返回结果

## 10. Service 调用关系

RecommendationService
  -> ProfileService
  -> RecallService
  -> RankingService
  -> RerankService
  -> ExplanationService

FeedbackService
  -> RecommendationResultMapper
  -> RecommendationExplanationMapper
  -> ProfileService