# 2026-04-21 Scenario Matrix And Baseline Comparison

## Goal
- 把推荐效果展示从“已有评估摘要”推进到“可讲清楚的基线对比 + 场景参数实验”。
- 支持 `topK / profileTopTagCount / rerankWeightScale` 三个参数维度。
- 让前端首页能够直接展示“完整链路 vs 标签重叠”的差异。

## Completed
- 新增参数上下文：
  - `src/main/java/com/lcj/campusreco/config/RecommendationTuningContext.java`
- 画像与重排接入参数上下文：
  - `ProfileServiceImpl`
  - `RerankServiceImpl`
- 新增场景矩阵导出：
  - `EvaluationScenarioExportVO`
  - `EvaluationScenarioMatrixService`
  - `EvaluationScenarioMatrixServiceImpl`
  - `POST /api/admin/evaluation/scenarios/export`
- 前端首页新增：
  - 基线对比卡片
  - 场景参数输入
  - 场景矩阵导出入口
- 测试新增/更新：
  - `RerankServiceImplTest`
  - `EvaluationScenarioMatrixServiceImplTest`
  - `ProfileServiceImplTest`
  - `ApiFlowIntegrationTest`

## Verification
- `powershell -ExecutionPolicy Bypass -File scripts/run-governance-checks.ps1`
- `mvn -gs .mvn/temp-settings.xml -Dmaven.repo.local=D:\.projects\xunzhiyin\.m2repo -Dsurefire.useFile=false test`

## Notes
- 这轮仍受 Maven/JDK 21 偶发编译器资源关闭抖动影响，但最终全量测试已通过。
