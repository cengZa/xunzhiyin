# Thesis Outline

## 模板层结构
- 中文摘要：`abstract_cn.md`
- 英文摘要：`abstract_en.md`
- 第 1 章 引言：`chapter1_intro.md`
- 第 2 章 相关工作 / 理论基础：`chapter2_related_work.md`
- 第 3 章 需求分析：`chapter3_analysis.md`
- 第 4 章 系统设计：`chapter4_design.md`
- 第 5 章 系统实现：`chapter5_implementation.md`
- 第 6 章 测试与结果分析：`chapter6_test.md`
- 第 7 章 结论：`chapter7_conclusion.md`
- 参考文献：`references.md`

## 建议写作顺序
1. `chapter3_analysis.md`
2. `chapter4_design.md`
3. `chapter5_implementation.md`
4. `chapter6_test.md`
5. `chapter1_intro.md`
6. `chapter2_related_work.md`
7. `chapter7_conclusion.md`
8. `abstract_cn.md`
9. `abstract_en.md`
10. `references.md`

## 各章节主责

### `chapter1_intro.md`
- 项目背景、研究意义、应用场景
- 研究问题与目标
- 技术路线总览
- 论文结构说明

### `chapter2_related_work.md`
- 协同过滤、兴趣建模、改进 TF-IDF、可解释推荐相关工作
- 为什么本项目不直接走复杂模型
- 与本项目技术路线的差异和选择理由

### `chapter3_analysis.md`
- 需求分析
- 项目范围与边界
- 功能 / 非功能需求
- 数据需求、测试需求、关键风险

### `chapter4_design.md`
- 总体架构
- 模块划分
- 推荐主链路设计
- 数据库与 Redis 设计
- 接口与解释机制设计

### `chapter5_implementation.md`
- Spring Boot 工程结构
- 画像、召回、排序、重排、解释、反馈的实现要点
- mock 数据、演示接口、离线评估导出等实现补充

### `chapter6_test.md`
- 测试环境与数据来源
- 功能验证
- 离线评估与参数实验
- 结果分析、限制与代理验证边界

### `chapter7_conclusion.md`
- 工作总结
- 实现价值与不足
- 后续优化方向

## 写作约束
- 模板格式要求由 `template_bridge.md` 负责解释。
- 章节内容必须回链到 `docs/` 中已存在的系统记录或 `generated/` 派生产物。
- 涉及量化目标时，必须区分“开题报告目标”“当前实际达成”“代理验证结果”。
