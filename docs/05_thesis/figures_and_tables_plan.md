# Figures And Tables Plan

本页规划论文正文建议使用的图表。当前不采集运行截图，只规划架构图、流程图、ER 图、关键表格和其放置位置，便于后续迁入 Word 模板。

## 1. 图表使用原则

- 论文正文优先使用架构图、流程图、ER 图、表结构和实验结果表。
- 运行页面截图不是必需项；如学校或导师要求展示系统运行效果，再由人工补截图。
- 图表必须服务正文论述，不为了数量堆砌。
- 图号、表号、题注和交叉引用最终以学校 Word 模板为准。
- 若图中包含模拟校园用户数据或代理指标，应在正文中说明数据来源和验证边界。

## 2. 建议图清单

| 图号建议 | 图题建议 | 放置章节 | 必要性 | 内容说明 | 来源依据 |
| --- | --- | --- | --- | --- | --- |
| 图 1-1 | 系统技术路线图 | 第 1 章 | 可选 | 展示从用户标签到反馈更新的总体路线 | `chapter1_intro.md` |
| 图 3-1 | 系统用例图 | 第 3 章 | 必要 | 展示校园用户、系统管理员和外部 LLM 服务的用例边界 | `chapter3_diagram_drafts.md` |
| 图 3-2 | 需求闭环流程图 | 第 3 章 | 可选 | 展示用户信息、画像、召回、排序重排、解释、反馈和画像更新闭环 | `chapter3_diagram_drafts.md` |
| 图 4-1 | 系统总体架构图 | 第 4 章 | 必要 | 展示 Controller、Service、Strategy、Mapper/Repository、MySQL、Redis 的分层关系 | `../02_design/system_architecture.md` |
| 图 4-3 | 数据库 ER 图 | 第 4 章 | 必要 | 展示 user、tag、user_tag_relation、user_profile、recommendation_result、recommendation_explanation、user_feedback 关系 | `../02_design/database_design.md` |
| 图 5-1 | 推荐主链路流程图 | 第 5 章 | 必要 | 展示画像、召回、排序、重排、解释、反馈流程 | `chapter5_diagram_drafts.md` |
| 图 5-2 | 系统核心类图 | 第 5 章 | 必要 | 展示 RecommendationServiceImpl 与画像、召回、排序、重排、解释、反馈等服务关系 | `chapter5_diagram_drafts.md` |
| 图 5-3 | 用户标签维护时序图 | 第 5 章 | 暂不采用 | 该流程属于画像生成前的数据准备环节，正文说明即可，避免图表重复 | `chapter5_implementation.md` |
| 图 5-4 | 用户画像生成时序图 | 第 5 章 | 必要 | 展示画像生成、权重计算、画像保存和缓存写入过程 | `chapter5_diagram_drafts.md` |
| 图 5-5 | 候选召回与相似度排序时序图 | 第 5 章 | 必要 | 展示 Top-K 标签、倒排召回、候选合并、相似度计算和标签贡献生成过程 | `chapter5_diagram_drafts.md` |
| 图 5-6 | 校园规则重排流程图 | 第 5 章 | 必要 | 展示场景模式、校园规则、可信连接分、探索位和最终 Top-K 结果 | `chapter5_diagram_drafts.md` |
| 图 5-7 | 推荐解释证据流图 | 第 5 章 | 已采用 | 展示排序贡献、规则命中、可信连接原因、规则解释、LLM 改写和回退关系 | `../generated/figures/ch5-7-explanation-evidence-flow.png` |
| 图 5-8 | 反馈更新时序图 | 第 5 章 | 已采用 | 展示反馈保存、解释证据读取和画像轻量更新过程 | `../generated/figures/ch5-8-feedback-update-sequence.png` |

## 3. 建议表清单

| 表号建议 | 表题建议 | 放置章节 | 必要性 | 内容说明 | 来源依据 |
| --- | --- | --- | --- | --- | --- |
| 表 3-1 | 系统功能范围说明 | 第 3 章 | 必要 | 汇总用户管理、标签管理、画像、召回、排序、重排、解释、反馈等范围 | `chapter3_analysis.md` |
| 表 3-2 | 系统非功能需求说明 | 第 3 章 | 必要 | 汇总可实现性、可维护性、可解释性、可验证性等需求 | `chapter3_analysis.md` |
| 表 4-1 | 系统分层结构说明 | 第 4 章 | 必要 | 汇总接口层、应用服务层、领域能力层、数据访问层 | `chapter4_design.md` |
| 表 4-2 | 核心模块职责表 | 第 4 章 | 必要 | 汇总用户、标签、画像、召回、排序、重排、解释、反馈、推荐编排模块 | `chapter4_design.md` |
| 表 4-3 | 核心数据表说明 | 第 4 章 | 必要 | 汇总核心表名与用途 | `chapter4_design.md` |
| 表 4-4 | 主要接口设计表 | 第 4 章 | 可选 | 汇总用户、画像、推荐、解释、反馈、演示评估接口 | `chapter4_design.md` |
| 表 5-1 | 核心机制设计说明 | 第 5 章 | 必要 | 汇总画像、召回、排序、重排、可信连接、解释、反馈机制 | `chapter5_implementation.md` |
| 表 5-2 | 第 5 章核心公式说明 | 第 5 章 | 必要 | 汇总标签权重、时间衰减、余弦相似度、最终分数和规则贡献公式 | `chapter5_implementation.md` |
| 表 5-3 | 核心服务实现说明 | 第 5 章 | 可选 | 汇总 ProfileService、RecallService、RankingService、RerankService、ExplanationService 等 | `chapter5_diagram_drafts.md` |
| 表 6-1 | 测试环境说明 | 第 6 章 | 必要 | 汇总 Java、Maven、H2、MySQL、Redis 和治理脚本 | `chapter6_test.md` |
| 表 6-2 | 测试数据规模 | 第 6 章 | 必要 | 展示模拟校园用户数据中的用户、标签和关系数量 | `chapter6_test.md` |
| 表 6-3 | 功能测试用例表 | 第 6 章 | 必要 | 按推荐链路汇总用户标签、画像、召回、排序、重排、解释、反馈和评估导出测试 | `chapter6_test.md` |
| 表 6-4 | 自动化测试结果 | 第 6 章 | 必要 | 展示 Tests run、Failures、Errors、Skipped、BUILD SUCCESS | `../generated/final-validation-latest.md` |
| 表 6-5 | 离线评估基线 | 第 6 章 | 必要 | 说明标签重叠、余弦排序、完整链路无可信分、完整链路含可信分四类基线 | `chapter6_test.md` |
| 表 6-6 | 离线评估结果 | 第 6 章 | 必要 | 展示 Precision@K、HitRate@K、解释覆盖率等代理指标 | `../generated/recommendation-evaluation-latest.md` |
| 表 6-7 | TopK 参数实验结果 | 第 6 章 | 可选 | 展示 TopK 变化对 Precision@K、HitRate@K 和解释覆盖率的影响 | `chapter6_test.md` |

## 4. 必须优先完成的图表

若时间有限，优先完成以下 8 个：

1. 图 3-1 系统用例图
2. 图 4-1 系统总体架构图
3. 图 4-3 数据库 ER 图
4. 图 5-1 推荐主链路流程图
5. 图 5-2 系统核心类图
6. 表 3-1 系统功能需求表
7. 表 4-2 核心数据表说明
8. 表 6-4 自动化测试结果

这 8 个图表覆盖需求、设计、数据、实现和验证，能支撑论文主体结构。

其中第 3 章底稿见 `chapter3_diagram_drafts.md`，第 4 章底稿见 `chapter4_diagram_drafts.md`，第 5 章底稿见 `chapter5_diagram_drafts.md`。

## 5. 不建议优先使用的材料

- 页面运行截图：除非导师明确要求，否则不作为论文主证据；若需要展示运行效果，优先放在第 6 章测试与运行效果部分。
- 原始 JSON 大段截图：论文中可用字段说明表替代。
- LLM 输出截图：容易让读者误解为 LLM 参与推荐排序。
- 过多参数实验表：当前数据为模拟校园用户数据，过度展开会放大代理验证的局限。

## 6. 图表绘制建议

### 6.1 系统总体架构图

建议结构：

```text
前端页面 / API 调用
        |
Controller 层
        |
Service 编排层
        |
Strategy 领域能力层
        |
Mapper / Repository 数据访问层
        |
MySQL + Redis
```

### 6.2 推荐主链路流程图

建议结构：

```text
用户标签 -> 用户画像 -> 倒排召回 -> 相似度排序
       -> 校园规则重排 -> 可信连接分 / 轻量探索
       -> 推荐解释 -> 用户反馈 -> 画像更新
```

### 6.3 数据库 ER 图

建议展示关系：

```text
user 1 -- n user_tag_relation n -- 1 tag
user 1 -- n user_profile
user 1 -- n recommendation_result
recommendation_result 1 -- n recommendation_explanation
recommendation_result 1 -- n user_feedback
```

## 7. Word 整合提醒

- 图题放在图下方，表题放在表上方，具体格式按学校模板。
- 迁入 Word 后再统一编号，Markdown 中的图号只是建议。
- 参考文献引用编号应在 Word 定稿阶段统一检查。
- 若最终删除某张图或表，需要同步检查正文中的“如图”“如表”引用。

