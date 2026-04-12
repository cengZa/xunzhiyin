# Test Cases

## 1. 编译与治理检查
- 在 Java 21 会话中运行 `powershell -ExecutionPolicy Bypass -File scripts/run-governance-checks.ps1`
- 期望：全部通过

## 1.1 前端演示页
- 访问 `/`
- 期望：返回 HTML 页面
- 重点检查：页面中包含 `CampusReco Demo Console`、`Recommendation Flow`、`Evaluation Matrix`、`Feedback Loop`

## 2. 单元测试
- 在 Java 21 会话中运行 `mvn test`
- 期望：画像构建、推荐编排、mock 数据初始化相关测试通过

## 3. 集成测试
- 在 Java 21 会话中运行 `mvn -Dtest=ApiFlowIntegrationTest test`
- 使用 `integration` profile，基于 H2 内存库自动建表，不依赖本地 MySQL
- 期望：Spring 上下文成功启动，并跑通 mock 初始化 -> 推荐 -> 反馈 -> 画像查询闭环

## 4. 本地依赖联调
- 在 Java 21 会话中设置本地环境变量后运行 `powershell -ExecutionPolicy Bypass -File scripts/run-local-env-verification.ps1`
- 关键环境变量：
  - `LOCAL_MYSQL_USER`
  - `LOCAL_MYSQL_PASSWORD`
  - `LOCAL_MYSQL_DB`
  - `LOCAL_REDIS_HOST`
  - `LOCAL_REDIS_PORT`
- 当前已验证配置：
  - MySQL：`localhost:3306`，用户 `root`
  - Redis：`127.0.0.1:6379`
- 期望：本地 MySQL 建库建表成功，`LocalMysqlFlowIntegrationTest` 通过

## 5. 演示数据初始化
- 调用 `POST /api/admin/mock/init`
- 期望：返回 tagCount / userCount / relationCount / profileRebuiltCount / recallIndexCount

## 6. 画像接口
- 调用 `POST /api/profiles/{userId}/build`
- 调用 `GET /api/profiles/{userId}`
- 期望：返回 profileVersion / profileJson / topkJson / updatedAt
- 重点检查：`profileJson` 不是占位值 `{}`，`topkJson` 不是占位值 `[]`

## 7. 推荐接口
- 调用 `GET /api/recommendations/{userId}?topK=3&useCache=false`
- 期望：返回 requestTraceId 和 items 列表
- 重点检查：items 中存在 targetUserId / finalScore / rankNo / explanation

## 8. 反馈接口
- 调用 `POST /api/recommendations/{userId}/feedback`
- 期望：返回 profileUpdated=true
- 后续再次推荐时，画像会被重建

## 9. 可选管理接口
- 调用 `POST /api/admin/profiles/rebuild-all`
- 调用 `POST /api/admin/recall/rebuild-index`
- 期望：返回重建数量

## 10. 离线评估摘要
- 调用 `GET /api/admin/evaluation/summary?topK=3`
- 期望：返回 `topK`、`activeUserCount`、`baselines`
- 重点检查：`baselines` 长度为 3，且包含 `tag_overlap`、`cosine_similarity`、`full_pipeline`

## 11. 离线评估报告
- 调用 `GET /api/admin/evaluation/report?topK=3`
- 期望：返回 Markdown 字符串
- 重点检查：结果中包含 `Recommendation Evaluation Summary`、`Precision@K` 和基线表头

## 12. 离线评估快照导出
- 调用 `POST /api/admin/evaluation/export?topK=3`
- 期望：返回 `fileName`、`filePath`、`baselineCount`
- 重点检查：目标文件实际存在，且包含 `Recommendation Evaluation Summary`

## 13. 参数实验矩阵导出
- 调用 `POST /api/admin/evaluation/experiments/export?topKs=3,5`
- 期望：返回 `fileName`、`filePath`、`experimentCount`、`topKValues`
- 重点检查：目标文件实际存在，且包含 `Recommendation Evaluation Matrix`
