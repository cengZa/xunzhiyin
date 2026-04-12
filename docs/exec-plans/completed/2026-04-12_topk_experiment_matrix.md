# 2026-04-12 TopK Experiment Matrix

## 背景
项目已经支持单次离线评估摘要和快照导出，但还缺少最小可用的参数实验矩阵能力。

## 目标
- 支持按多组 `topK` 参数批量导出实验矩阵
- 把参数实验结果落到稳定文件路径
- 更新接口、测试和文档入口

## 完成结果
- 新增 `EvaluationMatrixService`、`EvaluationMatrixServiceImpl`、`EvaluationMatrixExportVO`
- 新增接口 `POST /api/admin/evaluation/experiments/export`
- 新增生成物 `docs/generated/recommendation-evaluation-matrix-latest.md`
- 新增单元测试 `EvaluationMatrixServiceImplTest`
- 更新 `ApiFlowIntegrationTest` 覆盖参数实验矩阵导出

## 当前范围
- 当前仅覆盖 `topK` 参数实验
- 尚未扩展到画像 Top 标签数量和重排权重

## 验收结果
- `mvn test` 通过
- 治理检查通过
- 参数实验矩阵可落盘并被文档索引引用
