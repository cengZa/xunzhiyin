# Thesis Docs

`05_thesis/` 用于承接论文写作输出，不是实现任务的默认知识入口。

## 何时进入
- 编写论文章节、摘要、结论
- 整理答辩材料中的系统说明、实验描述、图表说明
- 校对论文文本与当前实现是否一致

## 建议阅读顺序
1. `thesis_outline.md`
2. `innovation_and_difficulty.md`
3. `thesis_consistency_check.md`
4. `figures_and_tables_plan.md`
5. `reference_sample_gap_review.md`
6. 与当前写作任务对应的章节文件
7. `../00_meta/source_opening_report.md`
8. `../04_test/` 中相关测试与评估文档
9. `references.md` 与 docs/参考文献 目录下的 README 页面

## 定稿前必读
- `thesis_consistency_check.md`
- `figures_and_tables_plan.md`
- `chapter3_diagram_drafts.md`
- `chapter4_diagram_drafts.md`
- `chapter5_diagram_drafts.md`
- `word_assembly_readiness.md`
- `innovation_and_difficulty.md`
- `../04_test/acceptance_traceability_matrix.md`
- `../04_test/final_demo_script.md`
- `../04_test/demo_evidence_samples.md`
- `../04_test/opening_report_validation_map.md`
- `../04_test/result_analysis.md`

## 当前章节状态
- 当前最终结构已确认改为工程型 7 章，详见 `thesis_outline.md`。第 5 章已合并为工程型“详细设计与实现”章节。
- `innovation_and_difficulty.md`: 已收口论文和答辩可用的创新点、重难点和过度表述边界。
- `chapter1_intro.md`: 已按工程型论文要求收敛为 1.1 至 1.4，包含项目背景与意义、核心概念定义、国内外研究现状与同类方案分析、本文主要工作和组织结构；第 1 章不再设置本章小结。
- `chapter2_related_work.md`: 已调整为相关理论及技术综述，补齐推荐系统、基于内容推荐、TF-IDF、协同过滤、可解释推荐、大语言模型辅助解释、混合推荐和技术路线选择。
- `chapter3_analysis.md`: 已按主要功能重写为需求分析章节，重点描述用户与标签维护、用户画像构建、匹配推荐生成、推荐解释与用户反馈等需求；数据需求已删除。
- `chapter4_design.md`: 已调整为第 4 章概要设计，聚焦系统架构、功能结构、数据库设计和 Redis 设计；接口组织设计已删除。
- `chapter5_implementation.md`: 已按模块重写为详细设计与实现章节，重点阐述用户画像构建、匹配推荐生成、推荐解释与用户反馈三个工作量较大的模块，并配套类图、时序图和界面图。
- `chapter6_test.md`: 已调整为最终第 6 章系统测试，已补测试环境、测试用例设计思路、功能性需求测试用例、非功能性需求测试用例和离线评估。
- `chapter7_conclusion.md`: 已按结论章要求重写为“全文总结”和“系统展望”两部分，不再设置本章小结。
- `abstract_cn.md`: 已完成阶段 6 修订，摘要与关键词已按当前 7 章结构和验证边界统一。
- `abstract_en.md`: 已完成阶段 6 修订，英文摘要与中文摘要术语和边界一致。
- `references.md`: 已扩充为第 1 章和第 2 章当前实际引用的 31 条参考文献，编号按正文首次出现顺序维护。
- `thesis_consistency_check.md`: 已补论文定稿前一致性检查清单。
- `word_assembly_readiness.md`: 已更新为当前 Word 组稿检查记录，明确第 1、3、4、5 章结构调整、参考范文图表样式和剩余人工确认项。
- `figures_and_tables_plan.md`: 已更新为当前正文实际采用的图表清单，覆盖第 3 章用例图、第 4 章架构与数据设计图、第 5 章类图、时序图和界面图。
- `reference_sample_gap_review.md`: 已记录当前论文与工程型参考范文在第 4 章组织、图表风格、表结构模板、界面截图和技术框架上的差异。
- `chapter3_diagram_drafts.md`: 已补第 3 章用例图、需求闭环图、功能需求表和非功能需求表底稿。
- `chapter4_diagram_drafts.md`: 已补第 4 章系统总体架构图和数据库 ER 图的可绘制底稿。
- `chapter5_diagram_drafts.md`: 已补第 5 章推荐主链路流程图、核心类图、画像时序图、推荐时序图、解释证据流图和反馈时序图底稿。
- `aigc_detection_review_2026-05-11.md`: 记录 2026-05-11 维普 AIGC 检测报告结论、章节风险分布和后续降风险修改顺序；原始报告位于 `../generated/aigc_reports/2026-05-11/`。

## 使用约束
- 论文表述必须回链到仓库内已实现、已验证的事实。
- 未完成或未验证的能力，不写成既成事实。
- 若实现已变化，先更新系统记录，再回写论文文本。

## 方便 agent 的工具
- 读取学校模板、开题报告或批注稿时，可运行：
  - `powershell -ExecutionPolicy Bypass -File scripts/extract-docx-text.ps1 -InputPath <docx路径>`
- 若要将抽取文本落盘，增加：
  - `-OutputPath <输出txt或md路径>`
