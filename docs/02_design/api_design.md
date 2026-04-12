# API Design

## 1. 设计原则
1. 接口按模块分组
2. 命名语义清晰
3. 优先满足推荐主链路与答辩演示
4. 输入输出字段与数据库设计保持一致

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

响应：
- 用户基础信息
- 可选返回标签摘要

### 2.3 为用户设置标签
`POST /api/users/{userId}/tags`

请求体：
- `tagIds[]`
- `sourceType`

响应：
- `success`

### 2.4 查询用户标签
`GET /api/users/{userId}/tags`

## 3. 画像模块
### 3.1 构建或重建用户画像
`POST /api/profiles/{userId}/build`

响应：
- `profileVersion`
- `topkTags`
- `updatedAt`

### 3.2 查询用户画像
`GET /api/profiles/{userId}`

响应：
- `profileJson`
- `topkJson`
- `profileVersion`

## 4. 推荐模块
### 4.1 获取推荐结果
`GET /api/recommendations/{userId}`

请求参数：
- `topK`
- `useCache=true/false`

响应：
- `requestTraceId`
- `items`
  - `targetUserId`
  - `finalScore`
  - `rankNo`
  - `explanation`

### 4.2 查询推荐详情
`GET /api/recommendations/{userId}/detail`

响应：
- `recallCandidatesCount`
- `rankingDetails`
- `rerankRuleHits`
- `explanationEvidence`

## 5. 解释模块
### 5.1 查询单条推荐解释
`GET /api/recommendations/{recommendationId}/explanation`

响应：
- `reasonText`
- `evidenceJson`
- `contributionJson`

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
- `success`
- `profileUpdated=true/false`

### 6.2 查询反馈记录
`GET /api/feedback/{userId}`

## 7. 管理与测试接口
### 7.1 初始化 mock 数据
`POST /api/admin/mock/init`

### 7.2 重建全部画像
`POST /api/admin/profiles/rebuild-all`

### 7.3 重建全部倒排索引
`POST /api/admin/recall/rebuild-index`

### 7.4 查询离线评估摘要
`GET /api/admin/evaluation/summary`

请求参数：
- `topK`

响应：
- `generatedAt`
- `topK`
- `activeUserCount`
- `baselines`

### 7.5 查询离线评估报告
`GET /api/admin/evaluation/report`

请求参数：
- `topK`

响应：
- Markdown 报告文本

### 7.6 导出离线评估快照
`POST /api/admin/evaluation/export`

请求参数：
- `topK`

响应：
- `fileName`
- `filePath`
- `baselineCount`
- `generatedAt`

### 7.7 导出参数实验矩阵
`POST /api/admin/evaluation/experiments/export`

请求参数：
- `topKs`

响应：
- `fileName`
- `filePath`
- `experimentCount`
- `topKValues`
