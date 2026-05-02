# Chapter 5 Diagram Drafts

本页提供第 5 章“详细设计与实现”图表底稿。图号、表号和题注仅为建议，最终编号以学校 Word 模板为准。第 5 章不再使用“主要包职责表”，也不展开接口参数和运行配置。

## 1. 图 5-1 推荐主链路流程图

### 1.1 论文题注建议

图 5-1 推荐主链路流程图

### 1.2 Mermaid 底稿

```mermaid
flowchart LR
    Tags["用户标签"]
    Profile["改进 TF-IDF<br/>画像构建"]
    TopK["Top-K<br/>核心标签"]
    Recall["倒排索引<br/>召回候选用户"]
    Rank["余弦相似度<br/>基础排序"]
    Rerank["校园规则重排<br/>年级 / 专业 / 社团"]
    Trust["可信连接分<br/>轻量探索"]
    Explain["推荐解释<br/>标签贡献 + 规则命中"]
    Result["Top-K<br/>推荐结果"]
    Feedback["用户反馈<br/>关注 / 忽略"]
    Update["画像轻量更新"]

    Tags --> Profile --> TopK --> Recall --> Rank --> Rerank --> Trust --> Explain --> Result
    Result --> Feedback --> Update
    Update -.-> Profile
```

## 2. 图 5-2 系统核心类图

### 2.1 论文题注建议

图 5-2 系统核心类图

### 2.2 Mermaid 底稿

```mermaid
classDiagram
    class RecommendationController
    class RecommendationService
    class RecommendationServiceImpl
    class ProfileService
    class RecallService
    class RankingService
    class RerankService
    class TrustScoreService
    class ExplorationService
    class ExplanationService
    class FeedbackService
    class RecommendationResultMapper
    class RecommendationExplanationMapper
    class UserFeedbackMapper
    class RecallIndexRepository
    class ProfileCacheRepository

    RecommendationController --> RecommendationService
    RecommendationService <|.. RecommendationServiceImpl
    RecommendationServiceImpl --> ProfileService
    RecommendationServiceImpl --> RecallService
    RecommendationServiceImpl --> RankingService
    RecommendationServiceImpl --> RerankService
    RecommendationServiceImpl --> TrustScoreService
    RecommendationServiceImpl --> ExplorationService
    RecommendationServiceImpl --> ExplanationService
    RecommendationServiceImpl --> RecommendationResultMapper
    RecommendationServiceImpl --> RecommendationExplanationMapper
    FeedbackService --> UserFeedbackMapper
    FeedbackService --> RecommendationResultMapper
    FeedbackService --> RecommendationExplanationMapper
    ProfileService --> ProfileCacheRepository
    RecallService --> RecallIndexRepository
```

## 3. 图 5-3 用户标签维护时序图

### 3.1 论文题注建议

图 5-3 用户标签维护时序图

### 3.2 Mermaid 底稿

```mermaid
sequenceDiagram
    actor User as 校园用户
    participant TagController
    participant TagServiceImpl
    participant UserTagRelationMapper
    participant ProfileServiceImpl

    User->>TagController: 提交标签新增或更新请求
    TagController->>TagServiceImpl: saveUserTags(userId, tags)
    TagServiceImpl->>UserTagRelationMapper: 保存用户标签关系
    UserTagRelationMapper-->>TagServiceImpl: 返回保存结果
    TagServiceImpl->>ProfileServiceImpl: 标记画像需刷新
    ProfileServiceImpl-->>TagServiceImpl: 返回处理结果
    TagServiceImpl-->>TagController: 返回标签维护结果
    TagController-->>User: 展示更新后的标签
```

## 4. 图 5-4 用户画像生成时序图

### 4.1 论文题注建议

图 5-4 用户画像生成时序图

### 4.2 Mermaid 底稿

```mermaid
sequenceDiagram
    actor User as 校园用户
    participant ProfileController
    participant ProfileServiceImpl
    participant UserTagRelationMapper
    participant ProfileWeightCalculator
    participant UserProfileMapper
    participant ProfileCacheRepository

    User->>ProfileController: 请求生成或刷新画像
    ProfileController->>ProfileServiceImpl: buildProfile(userId)
    ProfileServiceImpl->>UserTagRelationMapper: 查询用户标签关系
    UserTagRelationMapper-->>ProfileServiceImpl: 返回标签、时间、权重种子
    ProfileServiceImpl->>ProfileWeightCalculator: 计算 TF-IDF、时间衰减和 Top-K
    ProfileWeightCalculator-->>ProfileServiceImpl: 返回画像权重向量
    ProfileServiceImpl->>UserProfileMapper: 保存 profile_json 与 topk_json
    ProfileServiceImpl->>ProfileCacheRepository: 写入画像缓存
    ProfileServiceImpl-->>ProfileController: 返回画像结果
    ProfileController-->>User: 展示画像信息
```

## 5. 图 5-5 候选召回与相似度排序时序图

### 5.1 论文题注建议

图 5-5 候选召回与相似度排序时序图

### 5.2 Mermaid 底稿

```mermaid
sequenceDiagram
    participant RecommendationServiceImpl
    participant ProfileService
    participant RecallServiceImpl
    participant RecallIndexRepository
    participant RankingServiceImpl
    participant ProfileCacheRepository

    RecommendationServiceImpl->>ProfileService: 获取请求用户 Top-K 标签
    ProfileService-->>RecommendationServiceImpl: 返回画像与 Top-K 标签
    RecommendationServiceImpl->>RecallServiceImpl: recall(topKTags)
    RecallServiceImpl->>RecallIndexRepository: 按标签查询倒排索引
    RecallIndexRepository-->>RecallServiceImpl: 返回候选用户集合
    RecallServiceImpl-->>RecommendationServiceImpl: 返回去重后的候选用户
    RecommendationServiceImpl->>RankingServiceImpl: rank(targetProfile, candidates)
    RankingServiceImpl->>ProfileCacheRepository: 读取候选用户画像
    ProfileCacheRepository-->>RankingServiceImpl: 返回候选画像
    RankingServiceImpl-->>RecommendationServiceImpl: 返回相似度分数与标签贡献项
```

## 6. 图 5-6 校园规则重排流程图

### 6.1 论文题注建议

图 5-6 校园规则重排流程图

### 6.2 Mermaid 底稿

```mermaid
flowchart TB
    Base["基础排序结果"]
    Scenario["推荐场景模式<br/>学习 / 社团 / 兴趣"]
    Grade["年级差规则"]
    Major["专业相关规则"]
    Club["社团重合规则"]
    Campus["校园规则分"]
    Trust["可信连接分"]
    Explore["轻量探索位"]
    Final["最终 Top-K 推荐结果"]

    Base --> Scenario
    Scenario --> Grade --> Campus
    Scenario --> Major --> Campus
    Scenario --> Club --> Campus
    Campus --> Trust --> Explore --> Final
```

## 7. 图 5-7 推荐解释证据流图

### 7.1 论文题注建议

图 5-7 推荐解释证据流图

### 7.2 Mermaid 底稿

```mermaid
flowchart TB
    Rank["排序贡献项<br/>ContributionItemModel"]
    Rule["规则命中<br/>RuleHitModel"]
    Trust["可信连接原因<br/>TrustScoreResult"]
    Extractor["ExplanationEvidenceExtractor<br/>证据提取"]
    Template["ExplanationTemplateBuilder<br/>规则解释模板"]
    AI["AiExplanationClient<br/>可选解释改写"]
    Fallback["规则解释回退"]
    VO["ExplanationVO<br/>解释结果"]
    Store[("recommendation_explanation<br/>解释证据持久化")]

    Rank --> Extractor
    Rule --> Extractor
    Trust --> Extractor
    Extractor --> Template
    Template --> AI
    AI --> VO
    Template --> Fallback
    Fallback --> VO
    VO --> Store
```

## 8. 图 5-8 反馈更新时序图

### 8.1 论文题注建议

图 5-8 反馈更新时序图

### 8.2 Mermaid 底稿

```mermaid
sequenceDiagram
    actor User as 校园用户
    participant FeedbackController
    participant FeedbackServiceImpl
    participant UserFeedbackMapper
    participant RecommendationResultMapper
    participant RecommendationExplanationMapper
    participant ProfileServiceImpl
    participant UserProfileMapper

    User->>FeedbackController: 提交 follow 或 ignore 反馈
    FeedbackController->>FeedbackServiceImpl: submitFeedback(dto)
    FeedbackServiceImpl->>RecommendationResultMapper: 查询推荐结果
    RecommendationResultMapper-->>FeedbackServiceImpl: 返回推荐记录
    FeedbackServiceImpl->>RecommendationExplanationMapper: 查询解释证据
    RecommendationExplanationMapper-->>FeedbackServiceImpl: 返回贡献标签与规则证据
    FeedbackServiceImpl->>UserFeedbackMapper: 保存反馈记录
    FeedbackServiceImpl->>ProfileServiceImpl: 根据反馈证据轻量更新画像
    ProfileServiceImpl->>UserProfileMapper: 保存更新后的画像
    FeedbackServiceImpl-->>FeedbackController: 返回反馈处理结果
    FeedbackController-->>User: 展示提交结果
```

## 9. 表 5-3 核心服务实现说明

### 9.1 论文表题建议

表 5-3 核心服务实现说明

### 9.2 表格底稿

| 服务 | 主要实现类 | 核心职责 |
| --- | --- | --- |
| 用户画像服务 | `ProfileServiceImpl` | 构建用户画像、保存画像结果、维护画像缓存 |
| 候选召回服务 | `RecallServiceImpl` | 根据 Top-K 标签和倒排索引召回候选用户 |
| 排序服务 | `RankingServiceImpl` | 计算画像余弦相似度和标签贡献项 |
| 重排服务 | `RerankServiceImpl` | 应用年级、专业、社团等校园规则 |
| 可信连接服务 | `TrustScoreServiceImpl` | 根据资料完整度和匹配证据计算可信连接分 |
| 探索服务 | `ExplorationServiceImpl` | 在特定场景中保留轻量探索位 |
| 解释服务 | `ExplanationServiceImpl` | 提取解释证据、生成规则解释、处理外部解释改写回退 |
| 反馈服务 | `FeedbackServiceImpl` | 保存用户反馈并触发画像轻量更新 |
| 推荐编排服务 | `RecommendationServiceImpl` | 串联画像、召回、排序、重排、解释和持久化流程 |

## 10. Word 阶段处理项

- 本页图稿应在 Word 阶段重新绘制或由 Mermaid 导出后统一字体、线条和题注。
- 图 5-2 核心类图只展示论文所需主干关系，不展开全部 DTO、VO 和普通 Mapper。
- 图 5-7 和图 5-8 可以根据篇幅二选一保留；若导师关注创新点，优先保留图 5-7。
- 运行效果截图若必须加入论文，建议放入第 6 章“测试与运行效果”，并由人工提供截图或在本地运行系统后自动截取。
