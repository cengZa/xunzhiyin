# Thesis Template Bridge

本页用于把学校论文模板的章节要求，映射到仓库里的 thesis 文件和可引用证据。

## 模板来源
- 模板文件：`../本科毕设论文模板-论文主体.docx`
- 原始需求来源：`../22301095-林乘吉-开题报告.docx`

## Word 模板中的非正文部分
- 封面、版权使用授权书、诚信声明：保留在 Word 模板中处理，不在 markdown 章节文件中展开。
- 目录：由 Word 目录域自动生成，agent 只负责章节标题层级一致。
- 致谢：当前仓库未单独建 markdown 文件，建议在论文定稿阶段直接在 Word 中填写。

## 模板章节到仓库文件的映射

### 中文摘要
- 目标文件：`abstract_cn.md`
- 需要回答：
  - 研究目的
  - 研究方法
  - 成果
  - 结论
  - 创新点
- 主要证据来源：
  - `../00_meta/topic.md`
  - `../00_meta/source_opening_report.md`
  - `../04_test/result_analysis.md`
  - `../generated/recommendation-evaluation-latest.md`

### 英文摘要
- 目标文件：`abstract_en.md`
- 要求与中文摘要一致，但不能逐字机械翻译。

### 第 1 章 引言
- 目标文件：`chapter1_intro.md`
- 主要内容：
  - 校园社交匹配背景
  - 研究意义与实际价值
  - 国内外研究现状概述
  - 本文研究内容与结构
- 主要证据来源：
  - `../22301095-林乘吉-开题报告.docx`
  - `../00_meta/topic.md`
  - `../00_meta/source_opening_report.md`

### 第 2 章 相关工作 / 理论基础
- 目标文件：`chapter2_related_work.md`
- 主要内容：
  - 协同过滤相关研究
  - 用户兴趣建模
  - 改进 TF-IDF
  - 可解释推荐与 LLM 使用边界
- 主要证据来源：
  - `../22301095-林乘吉-开题报告.docx`
  - `references.md`

### 第 3 章 需求分析
- 目标文件：`chapter3_analysis.md`
- 主要内容：
  - 项目目标
  - 功能 / 非功能需求
  - 场景边界
  - 数据与测试需求
  - 风险分析
- 主要证据来源：
  - `../product-specs/index.md`
  - `../01_planning/project_scope.md`
  - `../01_planning/requirement_list.md`
  - `../01_planning/acceptance_criteria.md`
  - `../01_planning/risk_list.md`

### 第 4 章 系统设计
- 目标文件：`chapter4_design.md`
- 主要内容：
  - 总体架构
  - 模块设计
  - 推荐主链路设计
  - 数据库、Redis、接口设计
  - 解释机制设计
- 主要证据来源：
  - `../DESIGN.md`
  - `../02_design/`
  - `../03_backend/`

### 第 5 章 系统实现
- 目标文件：`chapter5_implementation.md`
- 主要内容：
  - Spring Boot 单体结构
  - 核心 service / strategy / infra 实现说明
  - mock 数据与演示支持能力
- 主要证据来源：
  - `../03_backend/backend_structure.md`
  - `../03_backend/service_design.md`
  - `../../src/main/java/`

### 第 6 章 测试与结果分析
- 目标文件：`chapter6_test.md`
- 主要内容：
  - 测试环境
  - 数据来源与模拟数据说明
  - 功能测试
  - 离线评估与参数实验
  - 结果分析与限制
- 主要证据来源：
  - `../04_test/test_cases.md`
  - `../04_test/opening_report_validation_map.md`
  - `../04_test/metrics_definition.md`
  - `../04_test/result_analysis.md`
  - `../generated/recommendation-evaluation-latest.md`
  - `../generated/recommendation-evaluation-matrix-latest.md`

### 第 7 章 结论
- 目标文件：`chapter7_conclusion.md`
- 主要内容：
  - 本文完成的工作
  - 达成效果
  - 局限与后续方向
- 主要证据来源：
  - `../00_meta/source_opening_report.md`
  - `../04_test/result_analysis.md`
  - `../QUALITY_SCORE.md`

### 参考文献
- 目标文件：`references.md`
- 当前主要参考来源：
  - 开题报告末尾参考文献
  - 后续真正引用到论文正文中的文献

## 模板格式提醒
- 中文摘要约 400 字，需包含目的、方法、成果、结论与创新点。
- 图、表、公式、参考文献格式以学校模板说明为准，agent 只负责内容与编号建议，不擅自宣称已满足 Word 排版细节。
- 引文标注需统一采用一套标准，建议全文使用 GB/T 7714 顺序编码制。

## 对 agent 的写作要求
- 先写事实，再写表述；先对齐实现，再追求文风。
- 若某章节所需证据尚未在仓库中落盘，应先补系统记录，再生成论文段落。
- 任何代理指标、模拟数据或答辩展示材料，都必须显式标注用途边界。
