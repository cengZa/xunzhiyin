# 2026-04-21 Demo Compare And Feedback Diff

## 目标
- 把答辩演示从“单条推荐展示”推进到“同一用户双视图对比 + 反馈前后变化”。
- 让老师不需要读原始 JSON，也能直接看到新算法匹配带来的排序差异。

## 完成内容
- 新增演示双视图对比能力：
  - `GET /api/admin/demo/compare?userId=2001&topK=3`
  - 返回 `标签重叠基线` 与 `完整链路` 两组结果
- 首页新增双视图对比区：
  - 对比仅标签重叠与完整链路的排序差异
  - 展示匹配标签、命中规则和算法标签
- 首页新增反馈前后变化区：
  - 展示反馈前后 Top 标签变化
  - 展示反馈前后推荐列表变化
- 修正文档失真：
  - 重新整理 [接口设计文档](../../02_design/api_design.md)
  - 重新整理 [前端演示页设计说明](../../design-docs/frontend_demo_console.md)
  - 补充 [测试用例清单](../../04_test/test_cases.md)

## 关键文件
- `src/main/java/com/lcj/campusreco/service/impl/DemoComparisonServiceImpl.java`
- `src/main/java/com/lcj/campusreco/controller/AdminController.java`
- `src/main/resources/static/index.html`
- `src/main/resources/static/app.js`
- `src/main/resources/static/app.css`
- `src/test/java/com/lcj/campusreco/service/impl/DemoComparisonServiceImplTest.java`
- `src/test/java/com/lcj/campusreco/ApiFlowIntegrationTest.java`

## 验证
- `powershell -ExecutionPolicy Bypass -File scripts/run-governance-checks.ps1`
- `mvn -gs .mvn/temp-settings.xml -Dmaven.repo.local=D:/.projects/xunzhiyin/.m2repo -Dsurefire.useFile=false test`

## 结果
- 双视图对比已成为首页默认展示的一部分。
- 反馈闭环已从接口级能力提升为页面级可视化能力。
- 当前演示流程已经更适合答辩讲解“新算法匹配”的创新点。
