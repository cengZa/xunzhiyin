# Figures And Tables Plan

本页记录当前论文正文采用的图表清单。图表编号、题注和正文引用应与 Word 组稿结果保持一致。

## 1. 图表使用原则

- 图表必须服务需求分析、概要设计、详细设计与实现、系统测试四条主线。
- 第 3 章以用例图和用例说明为主，不再展开数据需求。
- 第 4 章图 4-1、图 4-2、图 4-3 参考 `docs/更多参考/本科论文-工程型样例-刘.docx` 的图 4-1 至图 4-3 风格，分别表达系统架构、功能结构和 ER 关系。
- 第 5 章按重点模块给出类图、时序图和主要界面图，图 5-1 的类图风格参考工程型样例中的 UML 类图。
- 图题放在图下方，表题放在表上方；所有图、表附近必须有正文解释和编号引用。

## 2. 当前采用图清单

| 图号 | 图题 | 放置章节 | 内容说明 | 生成文件 |
| --- | --- | --- | --- | --- |
| 图 3-1 | 用户与标签维护用例图 | 第 3 章 | 展示校园用户和系统管理员在用户资料、兴趣标签、标签体系维护中的用例边界 | `docs/generated/figures/ch3-1-user-tag-usecase.png` |
| 图 3-2 | 画像构建与推荐生成用例图 | 第 3 章 | 展示画像构建、候选召回、相似度排序、校园规则重排和推荐结果查看需求 | `docs/generated/figures/ch3-2-recommendation-usecase.png` |
| 图 3-3 | 推荐解释用例图 | 第 3 章 | 展示推荐解释查看、解释证据读取和解释文本生成需求 | `docs/generated/figures/ch3-3-explanation-usecase.png` |
| 图 3-4 | 用户反馈与画像更新用例图 | 第 3 章 | 展示关注或忽略反馈、反馈记录保存、画像更新和后续推荐刷新需求 | `docs/generated/figures/ch3-4-feedback-usecase.png` |
| 图 4-1 | 系统架构图 | 第 4 章 | 参考工程型样例图 4-1，展示外部角色、系统分层、外部大语言模型服务、MySQL 和 Redis | `docs/generated/figures/ch4-1-system-architecture.png` |
| 图 4-2 | 系统功能结构图 | 第 4 章 | 参考工程型样例图 4-2，展示用户与标签维护、画像构建、推荐生成、推荐解释、反馈更新、运行评估等功能结构 | `docs/generated/figures/ch4-2-function-structure.png` |
| 图 4-3 | 系统 ER 图 | 第 4 章 | 参考工程型样例图 4-3，展示用户、标签、画像、推荐结果、推荐解释、用户反馈之间的核心关系 | `docs/generated/figures/ch4-3-er-diagram.png` |
| 图 5-1 | 用户画像构建模块类图 | 第 5 章 | 展示画像控制器、画像服务、权重计算策略、Mapper 和缓存仓储之间的关系 | `docs/generated/figures/ch5-1-profile-class-diagram.png` |
| 图 5-2 | 用户画像生成时序图 | 第 5 章 | 展示画像查询、标签读取、权重计算、画像保存和缓存写入过程 | `docs/generated/figures/ch5-2-profile-sequence.png` |
| 图 5-3 | 系统首页画像与推荐展示界面 | 第 5 章 | 展示目标用户画像、推荐摘要和推荐对象卡片 | `docs/generated/figures/ch5-3-home-screenshot.png` |
| 图 5-4 | 匹配推荐生成模块类图 | 第 5 章 | 展示推荐编排服务与画像、召回、排序、重排、可信连接、探索和解释服务的协作关系 | `docs/generated/figures/ch5-4-recommendation-class-diagram.png` |
| 图 5-5 | 匹配推荐生成时序图 | 第 5 章 | 展示推荐请求进入后依次完成画像获取、召回、排序、重排和推荐详情返回的过程 | `docs/generated/figures/ch5-5-recommendation-sequence.png` |
| 图 5-6 | 透明链路页面推荐生成阶段界面 | 第 5 章 | 展示输入标签、画像构建、候选召回、排序重排和最终解释的页面效果 | `docs/generated/figures/ch5-6-pipeline-screenshot.png` |
| 图 5-7 | 推荐解释与用户反馈模块类图 | 第 5 章 | 展示解释生成、证据抽取、模板构造、解释保存、反馈保存和画像更新协作关系 | `docs/generated/figures/ch5-7-explanation-feedback-class-diagram.png` |
| 图 5-8 | 用户反馈更新时序图 | 第 5 章 | 展示反馈提交、反馈保存、推荐证据读取、标签权重调整和画像重建过程 | `docs/generated/figures/ch5-8-feedback-sequence.png` |
| 图 5-9 | 推荐解释与反馈展示界面 | 第 5 章 | 展示推荐依据、规则证据、反馈前后画像变化和反馈入口 | `docs/generated/figures/ch5-9-feedback-screenshot.png` |

## 3. 当前采用表清单

| 表号 | 表题 | 放置章节 | 内容说明 | 生成文件或来源 |
| --- | --- | --- | --- | --- |
| 表 3-1 | 用户与标签维护用例说明 | 第 3 章 | 说明用户资料维护、标签维护和标签体系维护需求 | `chapter3_analysis.md` |
| 表 3-2 | 画像构建与推荐生成用例说明 | 第 3 章 | 说明画像构建、候选召回、排序重排和推荐结果查看需求 | `chapter3_analysis.md` |
| 表 3-3 | 推荐解释用例说明 | 第 3 章 | 说明推荐解释生成与查看需求 | `chapter3_analysis.md` |
| 表 3-4 | 用户反馈与画像更新用例说明 | 第 3 章 | 说明关注反馈、忽略反馈和画像更新需求 | `chapter3_analysis.md` |
| 表 3-5 | 非功能需求说明 | 第 3 章 | 说明可维护性、可解释性、可验证性、响应效率和数据一致性要求 | `chapter3_analysis.md` |
| 表 4-1 | 系统分层结构说明 | 第 4 章 | 说明接口层、应用服务层、领域能力层和数据访问层职责 | `chapter4_design.md` |
| 表 4-2 | 系统主要功能模块说明 | 第 4 章 | 说明第 4 章功能结构图中的核心模块职责 | `chapter4_design.md` |
| 表 4-3 | 核心数据表说明 | 第 4 章 | 说明用户、标签、画像、推荐、解释和反馈相关数据表用途 | `chapter4_design.md` |
| 表 4-4 | Redis 数据结构说明 | 第 4 章 | 说明画像缓存、推荐缓存和召回倒排索引用途 | `chapter4_design.md` |
| 表 5-1 | 推荐生成核心机制说明 | 第 5 章 | 说明候选召回、基础排序、场景重排、可信连接分和轻量探索机制 | `docs/generated/figures/ch5-table-1-core-mechanisms.png` |
| 表 5-2 | 推荐解释与反馈关键数据说明 | 第 5 章 | 说明标签贡献、规则命中、可信连接原因、反馈类型和画像版本用途 | `docs/generated/figures/ch5-table-2-explanation-feedback-data.png` |

## 4. 已确认的参考范文图表

- 已取得并检查 `docs/更多参考/本科论文-工程型样例-刘.docx` 的图 4-1、图 4-2、图 4-3。当前第 4 章图形采用其“系统边界 + 内部模块 + 外部系统 + 数据层”的表达方式。
- 已取得并检查该样例中的 UML 类图风格。当前第 5 章类图采用“类名 + 关键方法 + 依赖箭头”的表达方式。
- 表格样式优先贴近该样例中的用例说明表：表题置于表格上方，表头加粗，内容按用例编号、用例名称、参与者、描述、前置条件、基本流程等字段组织。

## 5. Word 整合提醒

- 若删除某张图或表，必须同步删除正文中的“如图”“如表”引用。
- 界面图在 Word 中应等比缩放，避免横向溢出页面。
- 第 5 章图较多，排版时应保证每个二级标题下先有过渡文字，再出现图或表。
- 图、表中文字应保持可读；若缩放后不可读，应换用更简洁版本或拆分为两张图。
