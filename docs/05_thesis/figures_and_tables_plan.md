# Figures And Tables Plan

本页记录当前论文正文采用的图表清单。图表编号、题注和正文引用应与 Word 组稿结果保持一致。

## 1. 图表使用原则

- 图表必须服务需求分析、概要设计、详细设计与实现、系统测试四条主线。
- 第 3 章以用例图和用例说明为主，不再展开数据需求。
- 第 4 章参考 `docs/更多参考/开发型论文 范文.pdf` 和 `docs/更多参考/本科论文-工程型样例-刘.docx`，分别表达系统整体架构、功能模块、核心流程和数据存储模型。
- 第 5 章按重点模块给出业务流程图、类图、时序图和主要界面图，业务流程图参考开发型范文“模块业务流程图”的组织方式，类图参考工程型样例中的 UML 类图。
- 图题放在图下方，表题放在表上方；所有图、表附近必须有正文解释和编号引用。

## 2. 当前采用图清单

| 图号 | 图题 | 放置章节 | 内容说明 | 生成文件 |
| --- | --- | --- | --- | --- |
| 图 3-1 | 用户与标签维护用例图 | 第 3 章 | 展示校园用户和系统管理员在用户资料、兴趣标签、标签体系维护中的用例边界 | `docs/generated/figures/ch3-1-user-tag-usecase.png` |
| 图 3-2 | 画像构建与推荐生成用例图 | 第 3 章 | 展示画像构建、候选召回、相似度排序、校园规则重排和推荐结果查看需求 | `docs/generated/figures/ch3-2-recommendation-usecase.png` |
| 图 3-3 | 推荐解释用例图 | 第 3 章 | 展示推荐解释查看、解释证据读取和解释文本生成需求 | `docs/generated/figures/ch3-3-explanation-usecase.png` |
| 图 3-4 | 用户反馈与画像更新用例图 | 第 3 章 | 展示关注或忽略反馈、反馈记录保存、画像更新和后续推荐刷新需求 | `docs/generated/figures/ch3-4-feedback-usecase.png` |
| 图 4-1 | 系统整体架构图 | 第 4 章 | 展示外部角色、系统边界、业务模块、MySQL、Redis 和外部大语言模型服务 | `docs/generated/figures/ch4-1-system-architecture.png` |
| 图 4-2 | 系统功能模块结构图 | 第 4 章 | 展示基础数据维护、画像构建、推荐生成、推荐解释与反馈等功能层级 | `docs/generated/figures/ch4-2-function-structure.png` |
| 图 4-3 | 系统核心流程图 | 第 4 章 | 展示用户登录、标签维护、画像构建、推荐生成、解释反馈和画像更新闭环 | `docs/generated/figures/ch4-3-core-flow.png` |
| 图 4-4 | 数据存储 ER 图（Peter Chen 表示法） | 第 4 章 | 使用矩形、菱形和椭圆展示用户、标签、画像、推荐结果、推荐解释和用户反馈的实体关系与关键属性 | `docs/generated/figures/ch4-4-conceptual-model.png` |
| 图 4-5 | 数据存储物理模型示意图 | 第 4 章 | 展示 MySQL 物理表、主键、索引和主要字段关系 | `docs/generated/figures/ch4-5-physical-model.png` |
| 图 5-1 | 用户画像构建模块业务流程图 | 第 5 章 | 展示画像构建请求、标签读取、权重计算、Top-K 截取、画像保存和缓存写入过程 | `docs/generated/figures/ch5-1-profile-business-flow.png` |
| 图 5-2 | 用户画像构建模块类图 | 第 5 章 | 展示画像控制器、画像服务、权重计算策略、Mapper 和缓存仓储之间的关系 | `docs/generated/figures/ch5-2-profile-class-diagram.png` |
| 图 5-3 | 用户画像生成时序图 | 第 5 章 | 展示画像查询、标签读取、权重计算、画像保存和缓存写入过程 | `docs/generated/figures/ch5-3-profile-sequence.png` |
| 图 5-4 | 系统首页画像与推荐展示界面 | 第 5 章 | 系统本地运行后截取的首页推荐展示区域，用于呈现推荐对象、匹配标签、分数构成和规则解释入口 | `docs/generated/figures/ch5-4-home-screenshot.png` |
| 图 5-5 | 匹配推荐生成模块业务流程图 | 第 5 章 | 展示推荐请求、画像读取、候选召回、排序重排、结果保存和详情返回过程 | `docs/generated/figures/ch5-5-recommendation-business-flow.png` |
| 图 5-6 | 匹配推荐生成模块类图 | 第 5 章 | 展示推荐编排服务与画像、召回、排序、重排、可信连接、探索和解释服务的协作关系 | `docs/generated/figures/ch5-6-recommendation-class-diagram.png` |
| 图 5-7 | 匹配推荐生成时序图 | 第 5 章 | 展示推荐请求进入后依次完成画像获取、召回、排序、重排和推荐详情返回的过程 | `docs/generated/figures/ch5-7-recommendation-sequence.png` |
| 图 5-8 | 透明链路页面推荐生成阶段界面 | 第 5 章 | 系统本地运行后截取的透明链路最终推荐区域，用于呈现最终 Top-K、规则命中和轻量探索位 | `docs/generated/figures/ch5-8-pipeline-screenshot.png` |
| 图 5-9 | 推荐解释与用户反馈模块业务流程图 | 第 5 章 | 展示证据读取、解释生成、外部服务改写、解释展示、反馈保存和画像更新过程 | `docs/generated/figures/ch5-9-explanation-feedback-business-flow.png` |
| 图 5-10 | 推荐解释与用户反馈模块类图 | 第 5 章 | 展示解释生成、证据抽取、模板构造、解释保存、反馈保存和画像更新协作关系 | `docs/generated/figures/ch5-10-explanation-feedback-class-diagram.png` |
| 图 5-11 | 用户反馈更新时序图 | 第 5 章 | 展示反馈提交、反馈保存、推荐证据读取、标签权重调整和画像重建过程 | `docs/generated/figures/ch5-11-feedback-sequence.png` |
| 图 5-12 | 推荐解释与反馈展示界面 | 第 5 章 | 系统本地运行后截取的解释展示区域，用于呈现规则解释与 LLM 改写解释的对照 | `docs/generated/figures/ch5-12-feedback-screenshot.png` |

## 3. 当前采用表清单

| 表号 | 表题 | 放置章节 | 内容说明 | 生成文件或来源 |
| --- | --- | --- | --- | --- |
| 表 3-1 | 用户与标签维护用例说明 | 第 3 章 | 说明用户资料维护、标签维护和标签体系维护需求 | `chapter3_analysis.md` |
| 表 3-2 | 画像构建与推荐生成用例说明 | 第 3 章 | 说明画像构建、候选召回、排序重排和推荐结果查看需求 | `chapter3_analysis.md` |
| 表 3-3 | 推荐解释用例说明 | 第 3 章 | 说明推荐解释生成与查看需求 | `chapter3_analysis.md` |
| 表 3-4 | 用户反馈与画像更新用例说明 | 第 3 章 | 说明关注反馈、忽略反馈和画像更新需求 | `chapter3_analysis.md` |
| 表 3-5 | 非功能需求说明 | 第 3 章 | 说明可维护性、可解释性、可验证性、响应效率和数据一致性要求 | `chapter3_analysis.md` |
| 表 4-1 | 用户基础信息表设计 | 第 4 章 | 按参考范文表 4-1 模板说明 user 表字段 | `chapter4_design.md` |
| 表 4-2 | 标签定义表设计 | 第 4 章 | 按参考范文表 4-1 模板说明 tag 表字段 | `chapter4_design.md` |
| 表 4-3 | 用户标签关系表设计 | 第 4 章 | 按参考范文表 4-1 模板说明 user_tag_relation 表字段 | `chapter4_design.md` |
| 表 4-4 | 用户画像表设计 | 第 4 章 | 按参考范文表 4-1 模板说明 user_profile 表字段 | `chapter4_design.md` |
| 表 4-5 | 推荐结果表设计 | 第 4 章 | 按参考范文表 4-1 模板说明 recommendation_result 表字段 | `chapter4_design.md` |
| 表 4-6 | 推荐解释表设计 | 第 4 章 | 按参考范文表 4-1 模板说明 recommendation_explanation 表字段 | `chapter4_design.md` |
| 表 4-7 | 用户反馈表设计 | 第 4 章 | 按参考范文表 4-1 模板说明 user_feedback 表字段 | `chapter4_design.md` |
| 表 4-8 | Redis 数据设计说明 | 第 4 章 | 说明画像缓存、推荐缓存和召回倒排索引用途 | `chapter4_design.md` |
| 表 5-1 | 推荐生成核心机制说明 | 第 5 章 | 说明候选召回、基础排序、场景重排、可信连接分和轻量探索机制 | `chapter5_implementation.md` |
| 表 5-2 | 推荐解释与反馈关键数据说明 | 第 5 章 | 说明标签贡献、规则命中、可信连接原因、反馈类型和画像版本用途 | `chapter5_implementation.md` |
| 表 6-1 | 测试环境说明 | 第 6 章 | 说明 JDK、构建工具、数据库、缓存组件、测试数据和结果核验方式 | `chapter6_test.md` |
| 表 6-2 | 用户与标签维护测试用例 | 第 6 章 | 按参考范文表 6-2 模板说明用户与标签维护功能测试 | `chapter6_test.md` |
| 表 6-3 | 画像构建与推荐生成测试用例 | 第 6 章 | 按参考范文表 6-2 模板说明画像构建与推荐生成功能测试 | `chapter6_test.md` |
| 表 6-4 | 推荐解释测试用例 | 第 6 章 | 按参考范文表 6-2 模板说明推荐解释功能测试 | `chapter6_test.md` |
| 表 6-5 | 用户反馈与画像更新测试用例 | 第 6 章 | 按参考范文表 6-2 模板说明反馈闭环功能测试 | `chapter6_test.md` |
| 表 6-6 | 透明链路与评估展示测试用例 | 第 6 章 | 按参考范文表 6-2 模板说明透明链路与评估展示功能测试 | `chapter6_test.md` |
| 表 6-7 | 响应可用性测试用例 | 第 6 章 | 按参考范文表 6-2 模板说明非功能性响应可用性测试 | `chapter6_test.md` |
| 表 6-8 | 可解释性测试用例 | 第 6 章 | 按参考范文表 6-2 模板说明推荐解释可追溯性测试 | `chapter6_test.md` |
| 表 6-9 | 可维护性与可验证性测试用例 | 第 6 章 | 按参考范文表 6-2 模板说明模块边界与材料复核测试 | `chapter6_test.md` |
| 表 6-10 | 离线评估指标说明 | 第 6 章 | 说明 Precision@K、NDCG@K、HitRate@K、覆盖率、响应时间和解释覆盖率的含义与用途 | `chapter6_test.md` |
| 表 6-11 | 离线评估基线 | 第 6 章 | 说明标签重叠、Jaccard、TF-IDF、改进 TF-IDF 和完整链路等评估基线 | `chapter6_test.md` |
| 表 6-12 | 离线评估结果 | 第 6 章 | 说明 Precision@K、NDCG@K、覆盖率和响应时间评估结果 | `chapter6_test.md` |
| 表 6-13 | TopK 参数实验结果 | 第 6 章 | 说明不同 TopK 下 Precision@K 和 NDCG@K 变化 | `chapter6_test.md` |

## 4. 已确认的参考范文图表

- 已取得并检查 `docs/更多参考/开发型论文 范文.pdf` 的系统整体架构图、功能模块结构图、系统核心流程图、数据存储模型图和模块业务流程图。当前第 4 章与第 5 章采用其“先流程、再结构、再实现”的组织方式。
- 本轮按要求将第 4 章概念模型和逻辑模型合并为一张 Peter Chen 表示法 ER 图，仅保留一张物理模型图和表结构详细设计，避免内容重复。
- 已取得并检查 `docs/更多参考/本科论文-工程型样例-刘.docx` 中的 UML 类图风格。当前第 5 章类图采用“类名 + 关键方法 + 依赖箭头”的表达方式。
- 第 4 章数据库表结构样式优先贴近该样例中的表 4-1：表题置于表格上方，随后给出“表名：xxx”，表格列为序号、字段名、类型、属性、描述。
- 正文 Markdown 表格在 Word 组稿时统一生成原生 Word 表格；第 6 章测试用例表按参考范文表 6-2 的四列用例模板处理。

## 5. Word 整合提醒

- 若删除某张图或表，必须同步删除正文中的“如图”“如表”引用。
- 界面图在 Word 中应等比缩放，避免横向溢出页面。
- 第 5 章图较多，排版时应保证每个二级标题下先有过渡文字，再出现图或表。
- 图、表中文字应保持可读；若缩放后不可读，应换用更简洁版本或拆分为两张图。
