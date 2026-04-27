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
5. 与当前写作任务对应的章节文件
6. `../00_meta/source_opening_report.md`
7. `../04_test/` 中相关测试与评估文档
8. `references.md` 与 docs/参考文献 目录下的 README 页面

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
- `chapter1_intro.md`: 已补研究背景、研究意义、研究内容、技术路线和论文结构，可作为第 1 章初稿继续润色。
- `chapter2_related_work.md`: 已调整为相关理论及技术综述，补齐推荐系统、基于内容推荐、TF-IDF、协同过滤、可解释推荐、LLM 辅助解释、混合推荐和技术路线选择。
- `chapter3_analysis.md`: 已完成一轮论文语体润色，强化需求来源、应用边界、功能分类、非功能约束和数据需求，可作为第 3 章正文初稿。
- `chapter4_design.md`: 已调整为第 4 章概要设计，聚焦总体架构、模块划分、数据存储和接口组织，算法细节已下沉到第 5 章。
- `chapter5_implementation.md`: 已合并算法机制与工程实现，作为最终第 5 章详细设计与实现初稿。
- `chapter6_test.md`: 已调整为最终第 6 章系统测试，已补测试环境、测试数据、功能测试用例、自动化测试结果和离线评估。
- `chapter7_conclusion.md`: 已重写为最终第 7 章总结与展望，已修正章节编号并压缩重复总结。
- `abstract_cn.md`: 已完成阶段 6 润色，摘要与关键词已按当前 7 章结构和验证边界统一。
- `abstract_en.md`: 已完成阶段 6 润色，英文摘要与中文摘要术语和边界一致。
- `references.md`: 已扩充为第 2 章当前实际引用的 15 条参考文献，编号按正文首次出现顺序维护。
- `thesis_consistency_check.md`: 已补论文定稿前一致性检查清单。
- `word_assembly_readiness.md`: 已补 Word 组稿前就绪检查，明确迁入顺序、标题规则、优先图表和人工后置项。
- `figures_and_tables_plan.md`: 已补论文图表规划，明确必要图表、可选图表和 Word 整合注意事项。
- `chapter3_diagram_drafts.md`: 已补第 3 章用例图、需求闭环图、功能需求表和非功能需求表底稿。
- `chapter4_diagram_drafts.md`: 已补第 4 章系统总体架构图和数据库 ER 图的可绘制底稿。
- `chapter5_diagram_drafts.md`: 已补第 5 章推荐主链路流程图、核心类图、画像时序图、推荐时序图、解释证据流图和反馈时序图底稿。

## 使用约束
- 论文表述必须回链到仓库内已实现、已验证的事实。
- 未完成或未验证的能力，不写成既成事实。
- 若实现已变化，先更新系统记录，再回写论文文本。

## 方便 agent 的工具
- 读取学校模板、开题报告或批注稿时，可运行：
  - `powershell -ExecutionPolicy Bypass -File scripts/extract-docx-text.ps1 -InputPath <docx路径>`
- 若要将抽取文本落盘，增加：
  - `-OutputPath <输出txt或md路径>`

