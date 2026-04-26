# Demo Evidence Samples

本页用于整理论文和答辩可复用的截图、接口样例和证据材料。当前先记录采集清单与样例骨架；真正截图可在本地后端启动后按本页执行。

## 1. 采集前准备

推荐环境：

- 后端端口：`8080`
- 默认用户：`2001`
- 默认 Top-K：`3`
- 默认场景：
  - 首页演示：`study_partner`
  - 透明链路：`interest_partner`

建议先调用：

```http
POST /api/admin/mock/init
```

预期稳定数据规模：

| 字段 | 预期值 |
| --- | --- |
| `tagCount` | 18 |
| `userCount` | 18 |
| `relationCount` | 86 |

## 2. 推荐截图清单

| 文件名建议 | 页面 / 接口 | 截图内容 | 论文用途 |
| --- | --- | --- | --- |
| `fig_home_overview.png` | `/` | 首页整体、推荐对象与演示侧栏 | 第 6 章系统实现、第 7 章功能测试 |
| `fig_home_recommendation_cards.png` | `/` | 推荐卡片中的分数、标签、规则和解释 | 第 5 章解释机制、第 7 章结果分析 |
| `fig_home_compare_views.png` | `/` | 标签重叠视图与完整链路视图对比 | 第 7 章离线评估与对比分析 |
| `fig_feedback_before_after.png` | `/` | 反馈前后 Top 标签和推荐列表变化 | 第 6 章反馈实现、第 7 章功能测试 |
| `fig_pipeline_overview.png` | `/pipeline.html` | 透明链路页整体 | 第 5 章推荐链路设计、第 6 章透明链路实现 |
| `fig_pipeline_profile_stage.png` | `/pipeline.html` | 输入标签、TF-IDF、时间衰减和 Top-K 画像 | 第 5 章画像设计、第 6 章画像实现 |
| `fig_pipeline_recall_ranking.png` | `/pipeline.html` | 召回候选池、排序公式和贡献明细 | 第 5 章召回排序设计 |
| `fig_pipeline_rerank_trust.png` | `/pipeline.html` | 重排规则、可信连接分、最终分数 | 第 5 章重排设计、第 6 章可信连接分实现 |
| `fig_pipeline_explanation_compare.png` | `/pipeline.html` | 规则解释与 LLM 改写解释对照 | 第 5 章解释机制、第 6 章 LLM 回退实现 |
| `fig_evaluation_summary.png` | `/api/admin/evaluation/summary?topK=3` 或页面展示 | 评估摘要、Precision@K、HitRate@K、解释覆盖率 | 第 7 章离线评估 |

## 3. 接口证据清单

### 3.1 推荐结果接口

请求：

```http
GET /api/recommendations/2001?topK=3&useCache=false&scenarioMode=study_partner
```

需要保留的字段：

```json
{
  "requestTraceId": "trace-id",
  "recallCandidatesCount": 9,
  "scenarioMode": "study_partner",
  "scenarioLabel": "学习搭子",
  "items": [
    {
      "recommendationId": 1,
      "targetUserId": 2002,
      "targetNickname": "候选用户昵称",
      "recallScore": 0.0,
      "rankScore": 0.0,
      "interestScore": 0.0,
      "rerankScore": 0.0,
      "campusScore": 0.0,
      "trustScore": 0.0,
      "finalScore": 0.0,
      "rankNo": 1,
      "matchedTags": ["算法", "自习"],
      "matchedRules": ["专业相关", "年级接近"],
      "trustReasons": ["资料完整", "共同标签较多"],
      "recommendationLabel": "高匹配",
      "explanation": "推荐理由文本"
    }
  ]
}
```

论文用途：
- 说明推荐结果不仅返回候选用户，还返回分数拆解、标签证据、规则命中和解释文本。

### 3.2 推荐详情接口

请求：

```http
GET /api/recommendations/2001/detail?scenarioMode=study_partner
```

需要保留的字段：

```json
{
  "rankingDetails": [
    {
      "targetUserId": 2002,
      "contributions": [
        {
          "tagId": 101,
          "tagName": "算法",
          "contribution": 0.15
        }
      ]
    }
  ],
  "rerankRuleHits": [
    {
      "targetUserId": 2002,
      "ruleHits": [
        {
          "ruleName": "MajorRelatedRule",
          "scoreDelta": 0.08,
          "reason": "专业相关"
        }
      ]
    }
  ],
  "explanationEvidence": [
    {
      "recommendationId": 1,
      "evidence": "共同标签与规则命中证据"
    }
  ]
}
```

论文用途：
- 证明解释机制有证据来源，不是独立编造推荐理由。

### 3.3 透明链路接口

请求：

```http
GET /api/admin/demo/pipeline?userId=2001&topK=3&scenarioMode=interest_partner
```

需要保留的字段：

```json
{
  "requestUser": {
    "userId": 2001,
    "nickname": "演示用户"
  },
  "scenarioStage": {
    "scenarioMode": "interest_partner",
    "objective": "兴趣同频推荐目标"
  },
  "inputTags": [
    {
      "tagName": "摄影",
      "tagTypeLabel": "兴趣"
    }
  ],
  "profileStage": {
    "weightFormula": "TF-IDF + timeDecay",
    "topTags": []
  },
  "recallStage": [
    {
      "recallFormula": "根据 Top-K 标签读取倒排索引",
      "matchedRecallTags": []
    }
  ],
  "rankingStage": [
    {
      "rankingFormula": "cosine similarity",
      "contributions": []
    }
  ],
  "rerankStage": [
    {
      "ruleDetails": [],
      "trustBreakdown": {}
    }
  ],
  "finalStage": [
    {
      "targetUserId": 2002,
      "exploration": true,
      "explorationReason": "探索原因"
    }
  ]
}
```

论文用途：
- 说明系统能展示从输入标签到最终推荐解释的完整过程。

### 3.4 单条解释接口

请求：

```http
GET /api/recommendations/{recommendationId}/explanation
```

需要保留的字段：

```json
{
  "recommendationId": 1,
  "reasonText": "最终展示解释",
  "ruleReasonText": "规则解释",
  "llmReasonText": "LLM 改写解释",
  "reasonSource": "rule",
  "evidenceJson": "{}",
  "contributionJson": "{}"
}
```

论文用途：
- 说明 LLM 只作为解释润色能力，且系统保留规则回退。

### 3.5 反馈接口

请求：

```http
POST /api/recommendations/2001/feedback
Content-Type: application/json

{
  "recommendationId": 1,
  "targetUserId": 2002,
  "feedbackType": "follow"
}
```

预期响应：

```json
{
  "profileUpdated": true
}
```

论文用途：
- 说明系统支持反馈采集和画像轻量更新。

## 4. 论文图表建议

| 编号建议 | 标题建议 | 来源 |
| --- | --- | --- |
| 图 6-1 | 系统首页推荐结果展示 | `fig_home_overview.png` |
| 图 6-2 | 透明推荐链路页面 | `fig_pipeline_overview.png` |
| 图 6-3 | 用户画像构建过程展示 | `fig_pipeline_profile_stage.png` |
| 图 6-4 | 重排规则与可信连接分展示 | `fig_pipeline_rerank_trust.png` |
| 图 6-5 | 推荐解释与 LLM 改写对照 | `fig_pipeline_explanation_compare.png` |
| 图 7-1 | 反馈前后推荐变化展示 | `fig_feedback_before_after.png` |
| 表 7-1 | 推荐接口关键字段样例 | 推荐结果接口 JSON |
| 表 7-2 | 离线评估指标摘要 | `../generated/recommendation-evaluation-latest.md` |

## 5. 采集注意事项

- 截图优先使用答辩模式，避免原始 JSON 占据主要画面。
- 论文需要说明 mock 数据和代理相关性规则，不把截图当作真实用户研究证据。
- 若截图中出现 LLM 改写内容，应同时保留规则解释来源，避免误解为 LLM 参与推荐排序。
- 最终 Word 排版中的图号、题注和跨页格式，以学校模板为准。
