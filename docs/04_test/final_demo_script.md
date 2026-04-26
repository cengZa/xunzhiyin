# Final Demo Script

本页固定答辩演示路径，避免现场临时选择接口和用户。

## 1. 演示准备

- Java: 21
- 后端端口: `8080`
- 本地 MySQL: `localhost:3306/campus_reco`
- Redis: `localhost:6379`
- 默认演示用户: `2001`
- 默认 Top-K: `3`
- 推荐场景:
  - `study_partner`: 学习搭子
  - `club_partner`: 社团搭子
  - `interest_partner`: 兴趣同频

## 2. 固定演示路径

### 步骤一：初始化演示数据

调用：

```http
POST /api/admin/mock/init
```

讲解重点：
- mock 数据用于答辩演示和离线验证。
- 当前稳定数据集包含用户、标签、用户标签关系和画像重建结果。

### 步骤二：展示首页

访问：

```http
GET /
```

讲解重点：
- 首页用于展示推荐结果、答辩故事线、双视图对比和反馈前后变化。
- 默认使用答辩模式，隐藏原始 JSON，便于非代码视角理解。

### 步骤三：展示推荐故事线

调用：

```http
GET /api/admin/demo/story?scenarioMode=study_partner
```

讲解重点：
- 先给出用户画像和推荐目标，再进入推荐链路。
- 说明系统不是泛社交推荐，而是校园匹配推荐。

### 步骤四：展示完整推荐结果

调用：

```http
GET /api/recommendations/2001?topK=3&useCache=false&scenarioMode=study_partner
```

讲解重点：
- 推荐结果同时返回兴趣分、校园规则分、可信连接分和最终分。
- 每条结果有 `matchedTags`、`matchedRules`、`trustReasons` 和 `explanation`。

### 步骤五：展示透明链路页面

访问：

```http
GET /pipeline.html
```

推荐参数：

```text
userId=2001
topK=3
scenarioMode=interest_partner
```

讲解重点：
- 依次展示输入标签、画像权重、召回、排序、重排、探索位和最终解释。
- `interest_partner` 场景可展示轻量探索位，但前 2 名主结果保持稳定。

### 步骤六：展示解释对照

调用：

```http
GET /api/recommendations/{recommendationId}/explanation
```

讲解重点：
- `ruleReasonText` 是规则解释。
- `llmReasonText` 只做表达润色。
- `reasonSource=rule` 表示规则回退，`reasonSource=llm` 表示 LLM 改写成功。
- LLM 不参与召回、排序、重排或反馈更新。

### 步骤七：提交反馈并展示变化

调用：

```http
POST /api/recommendations/2001/feedback
```

请求体包含：

```json
{
  "recommendationId": 1,
  "targetUserId": 2002,
  "feedbackType": "follow"
}
```

讲解重点：
- 反馈会记录用户行为。
- 系统会基于推荐证据对画像做轻量更新。
- 首页展示反馈前后 Top 标签和推荐列表变化。

### 步骤八：展示离线评估摘要

调用：

```http
GET /api/admin/evaluation/summary?topK=3
```

讲解重点：
- 当前评估基于 mock 数据和代理相关性规则。
- 评估用于证明链路可运行、可对比、可解释，不等同于真实线上用户研究。

## 3. 答辩讲法边界

可以说：
- 系统实现了用户画像、召回、排序、重排、解释和反馈闭环。
- 系统支持多场景推荐、可信连接分和轻量探索。
- 系统提供规则解释与 LLM 改写回退，保证解释来源可追溯。

不要说：
- 已完成真实用户问卷验证。
- 已证明线上推荐效果达到真实产品水平。
- LLM 是推荐排序算法核心。
- 所有开题报告量化目标都已正式达标。
