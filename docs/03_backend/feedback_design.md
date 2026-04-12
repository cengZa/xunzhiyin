# Feedback Design
# 反馈更新实现设计

## 1. 目标

根据用户对推荐结果的“关注”或“忽略”反馈，对兴趣画像做轻量级动态调整，增强系统适应性，同时避免画像剧烈波动。

## 2. 输入

- requestUserId
- recommendationId
- targetUserId
- feedbackType

## 3. 反馈类型

- follow：正向反馈
- ignore：负向反馈

## 4. 核心思路

反馈更新不直接“照搬目标用户标签”，而是只针对本次推荐中已经证明有效的“贡献标签”做轻量调整。

## 5. 更新来源

从 recommendation_explanation 或 contribution_json 中读取：
- shared_tags
- tag_contributions

## 6. 更新策略

### 6.1 follow
对贡献度最高的 2~3 个标签做正向增强：

newWeight = oldWeight * (1 + alpha)

alpha 建议：
- 0.05 ~ 0.15

### 6.2 ignore
对贡献度最高的 2~3 个标签做轻度降低：

newWeight = oldWeight * (1 - beta)

beta 建议：
- 0.03 ~ 0.10

## 7. 稳定性保护

1. 设置最大权重上限
2. 设置最小权重下限
3. 单次反馈调整幅度不宜过大
4. 可记录累计反馈次数，但不做复杂在线学习

## 8. 更新流程

1. 校验 recommendationId 与 requestUserId 匹配
2. 落库 user_feedback
3. 读取 recommendation_explanation 中的 evidence/contribution
4. 对 requestUserId 的画像相关标签做轻量调整
5. 重建 user_profile
6. 刷新 Redis 中的 profile:user:{userId}

## 9. 风险点

1. 噪声反馈导致画像偏移
2. 连续 ignore 导致画像塌缩
3. 单次反馈影响过大

## 10. 对策

1. 每次只调整少量高贡献标签
2. 保持增减幅度小
3. 可后续加“衰减回归”机制，但 MVP 阶段先不做复杂化