# 2026-04-12 Recommendation Evaluation Prep

## 背景
当前项目已经具备可运行的推荐主链路、H2 集成测试，以及本地 MySQL/Redis 联调能力，但还缺少面向答辩与论文的实验摘要、基线对比和可复用结果报告。

## 目标
- 基于现有 mock 数据产出可复用的推荐评估摘要
- 提供至少 3 组基线对比：标签重叠、纯排序得分、当前完整链路
- 生成可写入文档的 Markdown 实验结果

## 范围内
- 新增推荐评估服务、数据结构和管理接口
- 基于当前 mock 数据计算代理指标
- 新增测试覆盖指标汇总和 Markdown 导出
- 更新测试文档、结果分析和计划索引

## 范围外
- 真实人工标注实验
- 大规模数据集训练
- 深度学习或复杂学习排序模型

## 输入文档
- `../../04_test/metrics_definition.md`
- `../../04_test/mock_data_design.md`
- `../../04_test/result_analysis.md`
- `../../02_design/recommendation_pipeline.md`
- `../../PRODUCT_SENSE.md`

## 执行步骤
1. 定义评估输出结构和代理指标
2. 先写失败测试，覆盖基线汇总和 Markdown 导出
3. 实现评估服务与管理接口
4. 更新结果分析文档和测试说明
5. 跑治理检查和 Maven 测试

## 完成结果
- 新增 `EvaluationService`、`EvaluationServiceImpl`、`EvaluationBaselineVO`、`EvaluationSummaryVO`
- 新增管理接口：
- `GET /api/admin/evaluation/summary`
- `GET /api/admin/evaluation/report`
- 新增 `EvaluationServiceImplTest`
- 扩展 `ApiFlowIntegrationTest` 覆盖评估摘要与报告接口
- 更新 `../../04_test/metrics_definition.md`、`../../04_test/result_analysis.md`、`../../04_test/test_cases.md`
- Maven `test` 与治理检查均已通过

## 风险
- mock 数据规模较小，评估结果更适合作为演示和草案
- 代理相关性规则必须保持简单，否则会引入无法自证的复杂假设

## 验收结果
- 仓库内已存在可调用的推荐评估服务
- 已可输出 3 组基线对比结果
- 已可生成 Markdown 格式报告文本
- 新增测试通过，现有测试未回退

## 决策日志
- 2026-04-12：优先补“实验摘要与报告产物”，而不是继续扩大业务面
