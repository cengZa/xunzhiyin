# Generated Index

用于登记由代码、脚本或结构化过程派生出的资料。

## 当前候选
- `../03_backend/schema.sql`: 数据库建表脚本，可视为设计派生产物
- `recommendation-evaluation-latest.md`: 运行 `POST /api/admin/evaluation/export` 后生成的最新评估快照
- `recommendation-evaluation-matrix-latest.md`: 运行 `POST /api/admin/evaluation/experiments/export` 后生成的最新参数实验矩阵
- `recommendation-scenario-matrix-latest.md`: 运行 `POST /api/admin/evaluation/scenarios/export` 后生成的最新场景参数矩阵
- `final-validation-latest.md`: 2026-04-25 最终治理检查与 Maven 测试结果
- `thesis_word_7chapter_draft.docx`: 基于学校论文主体模板生成的 7 章 Word 组稿候选；当前表格先以字段说明块呈现，最终定稿时需在 Word 内按模板表格样式重排
- `thesis_word_7chapter_draft_render/`: 上述 Word 草稿的 artifact-tool 渲染检查输出，仅用于内部版式 QA
- `thesis_word_final_candidate.docx`: 旧 Word 定稿整理候选；已由 v3 候选稿替代。
- `thesis_word_final_candidate_render/`: 旧定稿候选的 artifact-tool 渲染检查输出，仅用于内部版式 QA。
- `thesis_word_final_candidate_v3.docx`: 旧 Word 定稿候选；已由 v4 候选稿替代。
- `thesis_word_final_candidate_v3_render/`: v3 Word 候选稿的 artifact-tool 渲染检查输出，仅用于内部版式 QA。
- `thesis_word_final_candidate_v4.docx`: 旧 Word 定稿候选；已由 v5 候选稿替代。
- `thesis_word_final_candidate_v4_render/`: v4 Word 候选稿的 artifact-tool 渲染检查输出，仅用于内部版式 QA。
- `figures/`: Word 组稿用图像目录，当前包含第 3 章用例图、第 4 章架构/功能结构/ER 图、第 5 章类图/时序图/界面图、第 5 章专用表格 PNG，以及正文 Markdown 表格自动生成的 `table-*.png`。
- `interface-captures/`: 第 5 章界面图的本地采图 HTML，仅作为 `figures/ch5-3-home-screenshot.png`、`figures/ch5-6-pipeline-screenshot.png`、`figures/ch5-9-feedback-screenshot.png` 的生成来源。
- `thesis_word_final_candidate_v5.docx`: 旧 Word 定稿候选；已由 v6 候选稿替代。
- `thesis_word_final_candidate_v5_render/`: v5 Word 候选稿的 artifact-tool 渲染检查输出，仅用于内部版式 QA。
- `thesis_word_final_candidate_v6.docx`: 旧 Word 定稿候选；已由 v7 候选稿替代。
- `thesis_word_final_candidate_v6_render/`: v6 Word 候选稿的 artifact-tool 渲染检查输出，仅用于内部版式 QA。
- `thesis_word_final_candidate_v7.docx`: 旧 Word 定稿候选；已由 v8 候选稿替代。
- `thesis_word_final_candidate_v7_render/`: v7 Word 候选稿的 artifact-tool 渲染检查输出，仅用于内部版式 QA。
- `thesis_word_final_candidate_v8.docx`: 旧 Word 定稿候选；将部分图表落成正式 PNG，并同步静态目录页码与实际页脚，已由 v9 候选稿替代。
- `thesis_word_final_candidate_v8_render/`: v8 Word 候选稿的 artifact-tool 渲染检查输出，仅用于内部版式 QA。
- `thesis_word_final_candidate_v9.docx`: 当前 Word 定稿候选；基于 v8 继续重写第 1、3、4、5 章，补齐第 3 章用例图、第 4 章系统架构/功能结构/ER 图、第 5 章类图/时序图/界面图，并修正正文标题为单份章节编号。
- `thesis_word_final_candidate_v9_render/`: v9 Word 候选稿的 artifact-tool 渲染检查输出，仅用于内部版式 QA。

## 规则
- 生成产物应说明来源。
- 若产物会被 agent 消费，应提供稳定入口，而不是散落在聊天记录里。
- 采用 7 章工程型结构后，旧 8 章 Word 组稿产物应视为过时生成物，不再保留。
- artifact-tool 对学校模板和工程型样例中的 Word 原生表格存在竖排渲染缺陷；自动候选稿中已定稿的关键表格可先生成为样例风格 PNG 插入，未定稿表格仍用醒目占位块保留字段底稿，最终 Word 中再按 `docs/更多参考/本科论文-工程型样例-刘.docx` 表 2-1 或表 3-2 样式重排。
- 论文定稿表述不使用未解释缩写、过程性自述或削弱工程真实性的表述；如需需求编号，优先使用“功能-xx”“非功能-xx”等中文编号。
