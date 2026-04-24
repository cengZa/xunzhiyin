# 2026-04-20 Frontend Product Homepage

## Goal
- 将前端从工程演示控制台调整为更像真实产品首页的展示页，同时保留演示侧栏。
- 把固定演示用户 `2001` 的故事线、算法亮点、推荐拆解与解释证据直接呈现在首页。
- 清理对外暴露文案中的乱码和英文残留。

## Completed
- 重写静态首页：
  - `src/main/resources/static/index.html`
  - `src/main/resources/static/app.css`
  - `src/main/resources/static/app.js`
- 首页结构调整为：
  - Hero 区
  - 答辩故事线
  - 用户画像
  - 离线评估
  - 推荐对象与算法拆解
  - 推荐解释
  - 演示侧栏
- 补充并清理对外暴露中文文本：
  - `DemoStoryServiceImpl`
  - `EvaluationServiceImpl`
  - `EvaluationMatrixServiceImpl`
  - `ExplanationTemplateBuilder`
  - `ClubOverlapRule`
  - `MajorRelatedRule`
  - `GradeDiffRule`
- 更新集成测试首页断言与评估报告断言：
  - `ApiFlowIntegrationTest`
- 同步更新文档：
  - `docs/design-docs/frontend_demo_console.md`
  - `docs/04_test/result_analysis.md`
  - `docs/04_test/test_cases.md`

## Verification
- `powershell -ExecutionPolicy Bypass -File scripts/run-governance-checks.ps1`
- `mvn -gs .mvn/temp-settings.xml -Dmaven.repo.local=D:\.projects\xunzhiyin\.m2repo -Dsurefire.useFile=false test`

## Notes
- 本机上 Maven/JDK 21 仍可能出现一次性的 `无法关闭编译器资源` 抖动；本轮通过先执行 `test-compile` 再执行 `test` 拿到稳定通过结果。
