# API Design

## 1. 设计原则
1. 接口围绕校园用户匹配推荐主链路组织，不扩展到内容流或复杂社交功能。
2. 对外返回优先保证可读性，避免把低质量 `null` 和内部实现细节直接暴露给前端。
3. 推荐结果不仅返回最终排序，还返回“为什么推荐”“为什么排在前面”的拆解信息。
4. 演示、评估、答辩相关接口必须能被首页和演示侧栏直接消费。

## 2. 用户与标签模块
### 2.1 创建用户
`POST /api/users`

请求体：
- `nickname`
- `grade`
- `major`
- `college`
- `bio`

响应：
- `userId`

### 2.2 查询用户详情
`GET /api/users/{userId}`

### 2.3 绑定用户标签
`POST /api/users/{userId}/tags`

请求体：
- `tagIds[]`
- `sourceType`

### 2.4 查询用户标签
`GET /api/users/{userId}/tags`

## 3. 画像模块
### 3.1 构建或重建画像
`POST /api/profiles/{userId}/build`

响应：
- `profileVersion`
- `profileJson`
- `topkJson`
- `updatedAt`

### 3.2 查询画像
`GET /api/profiles/{userId}`

## 4. 推荐模块
### 4.1 获取推荐结果
`GET /api/recommendations/{userId}`

请求参数：
- `topK`
- `useCache=true/false`
- `scenarioMode`
  - `study_partner`
  - `club_partner`
  - `interest_partner`

响应：
- `requestTraceId`
- `recallCandidatesCount`
- `scenarioMode`
- `scenarioLabel`
- `rankingDetails`
- `rerankRuleHits`
- `explanationEvidence`
- `items`
  - `recommendationId`
  - `targetUserId`
  - `targetNickname`
  - `recallScore`
  - `rankScore`
  - `interestScore`
  - `rerankScore`
  - `campusScore`
  - `trustScore`
  - `finalScore`
  - `rankNo`
  - `scenarioMode`
  - `scenarioLabel`
  - `matchedTags`
  - `matchedRules`
  - `trustReasons`
  - `recommendationLabel`
  - `explanation`

### 4.2 查询推荐详情
`GET /api/recommendations/{userId}/detail`

请求参数：
- `scenarioMode`

响应：
- `requestTraceId`
- `recallCandidatesCount`
- `scenarioMode`
- `scenarioLabel`
- `items`
- `rankingDetails`
- `rerankRuleHits`
- `explanationEvidence`

## 5. 解释模块
### 5.1 查询单条推荐解释
`GET /api/recommendations/{recommendationId}/explanation`

响应：
- `recommendationId`
- `reasonText`
- `evidenceJson`
- `contributionJson`
- `evidence`
- `contribution`

## 6. 反馈模块
### 6.1 提交反馈
`POST /api/recommendations/{userId}/feedback`

请求体：
- `recommendationId`
- `targetUserId`
- `feedbackType`
  - `follow`
  - `ignore`

响应：
- `profileUpdated=true/false`

### 6.2 查询反馈记录
`GET /api/feedback/{userId}`

## 7. 管理与演示接口
### 7.1 初始化 mock 数据
`POST /api/admin/mock/init`

### 7.2 重建全部画像
`POST /api/admin/profiles/rebuild-all`

### 7.3 重建召回索引
`POST /api/admin/recall/rebuild-index`

### 7.4 演示故事线
`GET /api/admin/demo/story`

请求参数：
- `scenarioMode`

响应：
- `demoUserId`
- `scenarioMode`
- `scenarioLabel`
- `storyTitle`
- `personaSummary`
- `storyNarrative`
- `algorithmHighlights`
- `expectedCandidateIds`
- `candidateSpotlights`

### 7.5 演示双视图对比
`GET /api/admin/demo/compare`

请求参数：
- `userId`
- `topK`
- `scenarioMode`

响应：
- `userId`
- `topK`
- `candidateCount`
- `scenarioMode`
- `scenarioLabel`
- `tagOverlapView`
  - `viewCode`
  - `viewName`
  - `summary`
  - `items`
- `fullPipelineView`
  - `viewCode`
  - `viewName`
  - `summary`
  - `items`

### 7.6 离线评估摘要
`GET /api/admin/evaluation/summary`

响应重点：
- `scenarioMode`
- `scenarioLabel`
- `baselines`
  - `tag_overlap`
  - `cosine_similarity`
  - `full_pipeline_no_trust`
  - `full_pipeline_with_trust`

### 7.7 离线评估报告
`GET /api/admin/evaluation/report`

### 7.8 导出离线评估快照
`POST /api/admin/evaluation/export`

### 7.9 导出 TopK 实验矩阵
`POST /api/admin/evaluation/experiments/export`

### 7.10 导出场景参数矩阵
`POST /api/admin/evaluation/scenarios/export`

请求参数：
- `scenarioModes`
- `topKs`
- `profileTopTagCounts`
- `rerankWeightScales`

响应：
- `fileName`
- `filePath`
- `scenarioModes`
- `scenarioCount`
- `topKValues`
- `profileTopTagCounts`
- `rerankWeightScales`
