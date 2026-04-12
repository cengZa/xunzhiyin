# 2026-04-12 Mock Expansion And Snapshot Export

## 背景
上一阶段已经具备离线评估摘要与 Markdown 报告能力，但 mock 数据规模偏小，且评估结果没有稳定落盘入口。

## 目标
- 扩充 mock 数据分布，使评估样本更接近真实校园兴趣圈层
- 提供评估快照导出能力，将结果落到稳定路径
- 更新集成测试与系统记录，确保功能和文档一致

## 范围内
- 扩充标签、用户和用户标签关系
- 新增评估快照导出服务和管理接口
- 更新 H2 集成测试与单元测试
- 更新测试和 API 文档

## 完成结果
- mock 数据已扩充到 12 个标签、12 个用户、48 条关系
- 新增 `POST /api/admin/evaluation/export`
- 新增 `EvaluationSnapshotService` 与 `EvaluationExportVO`
- 集成测试已验证评估快照会落到 `target` 下的专用临时导出目录
- 文档已补充快照导出入口与更大的 mock 数据说明

## 验收结果
- `mvn test` 通过
- 治理检查通过
- 新增测试 `EvaluationSnapshotServiceImplTest` 通过

## 风险
- 当前评估快照仍基于小规模 mock 数据，只适合演示和实验草案
- Windows + JDK 21 + Maven 在 `testCompile` 阶段仍偶发出现编译器资源关闭异常，需要先单独跑一次 `test-compile` 时才能稳定复现通过
