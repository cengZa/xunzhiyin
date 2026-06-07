# Test Cases

## 1. 编译与治理检查
- 在 Java 21 会话中运行 `powershell -ExecutionPolicy Bypass -File scripts/run-governance-checks.ps1`
- 期望：全部通过

## 1.1 前端演示页
- 访问 `/`
- 期望：返回 HTML 页面
- 重点检查：页面中包含 `CampusReco 校园匹配首页`、`答辩故事线`、`推荐对象与算法拆解`、`演示侧栏`

## 2. 单元测试
- 在 Java 21 会话中运行 `mvn test`
- 期望：画像构建、推荐编排、mock 数据初始化相关测试通过

## 3. 集成测试
- 在 Java 21 会话中运行 `mvn -Dtest=ApiFlowIntegrationTest test`
- 使用 `integration` profile，基于 H2 内存库自动建表，不依赖本地 MySQL
- 期望：Spring 上下文成功启动，并跑通 mock 初始化 -> 推荐 -> 反馈 -> 画像查询闭环

## 4. 本地依赖联调
- 在 Java 21 会话中直接运行 `powershell -ExecutionPolicy Bypass -File scripts/run-local-env-verification.ps1`
- 当前已验证配置：
  - MySQL：`localhost:3306`，用户 `root`
  - 密码：`root`
  - 数据库：`campus_reco`
  - Redis：`127.0.0.1:6379`
- 期望：本地 MySQL 建库建表成功，`LocalMysqlFlowIntegrationTest` 通过

## 5. 演示数据初始化
- 调用 `POST /api/admin/mock/init`
- 期望：返回 tagCount / userCount / relationCount / profileRebuiltCount / recallIndexCount
- 当前稳定答辩数据集期望值：
  - `tagCount=36`
  - `userCount=18`
  - `relationCount=130`

## 5.1 演示故事线接口
- 调用 `GET /api/admin/demo/story?scenarioMode=study_partner`
- 期望：返回 `demoUserId=2001`
- 重点检查：
  - 包含 `scenarioMode`、`scenarioLabel`
  - 包含 `algorithmHighlights`、`expectedCandidateIds` 和 `candidateSpotlights`

## 5.2 演示双视图对比接口
- 调用 `GET /api/admin/demo/compare?userId=2001&topK=3&scenarioMode=study_partner`
- 期望：返回 `tagOverlapView` 和 `fullPipelineView`
- 重点检查：
  - 顶层返回 `scenarioMode=study_partner`
  - `tagOverlapView.viewCode=tag_overlap`
  - `fullPipelineView.viewCode=full_pipeline`
  - 两个视图首位推荐对象顺序存在差异
  - 完整链路视图结果项包含 `targetNickname`、`matchedTags`、`matchedRules`、`trustScore`、`recommendationLabel`

## 6. 画像接口
- 调用 `POST /api/profiles/{userId}/build`
- 调用 `GET /api/profiles/{userId}`
- 期望：返回 profileVersion / profileJson / topkJson / updatedAt
- 重点检查：`profileJson` 不是占位值 `{}`，`topkJson` 不是占位值 `[]`

## 7. 推荐接口
- 调用 `GET /api/recommendations/{userId}?topK=3&useCache=false&scenarioMode=study_partner`
- 期望：返回 requestTraceId 和 items 列表
- 重点检查：
  - 顶层存在 `scenarioMode` / `scenarioLabel`
  - items 中存在 `targetUserId` / `targetNickname` / `recallScore` / `rankScore` / `interestScore` / `rerankScore` / `campusScore` / `trustScore` / `finalScore`
  - items 中存在 `matchedTags` / `matchedRules` / `trustReasons` / `recommendationLabel` / `explanation`

## 7.1 轻量探索
- 调用 `GET /api/recommendations/{userId}?topK=3&useCache=false&scenarioMode=interest_partner`
- 期望：最后 1 个候选可能被标记为探索位
- 重点检查：
  - `items[2].exploration=true`
  - `items[2].explorationScore` 为数字
  - `items[2].explorationReason` 非空
  - `items[0]` 与 `items[1]` 仍保持主排序稳定

## 8. 反馈接口
- 调用 `POST /api/recommendations/{userId}/feedback`
- 期望：返回 profileUpdated=true
- 后续再次推荐时，画像会被重建

## 8.1 反馈前后变化展示
- 在首页上选择一条推荐记录提交 `follow`
- 期望：页面刷新画像、推荐、双视图对比和反馈前后变化区
- 重点检查：
  - `反馈前 Top 标签` 与 `反馈后 Top 标签` 有可读差异
  - `反馈前推荐列表` 与 `反馈后推荐列表` 至少一项顺序或说明发生变化

## 9. 可选管理接口
- 调用 `POST /api/admin/profiles/rebuild-all`
- 调用 `POST /api/admin/recall/rebuild-index`
- 期望：返回重建数量

## 10. 离线评估摘要
- 调用 `GET /api/admin/evaluation/summary?topK=3`
- 期望：返回 `topK`、`activeUserCount`、`baselines`
- 重点检查：
  - 返回 `scenarioMode` / `scenarioLabel`
  - `baselines` 长度为 4
  - 包含 `tag_overlap`、`cosine_similarity`、`full_pipeline_no_trust`、`full_pipeline_with_trust`

## 11. 离线评估报告
- 调用 `GET /api/admin/evaluation/report?topK=3`
- 期望：返回 Markdown 字符串
- 重点检查：结果中包含 `推荐评估摘要`、`Precision@K` 和基线表头

## 11.1 推荐详情接口
- 调用 `GET /api/recommendations/{userId}/detail?scenarioMode=study_partner`
- 期望：返回 `items`、`rankingDetails`、`rerankRuleHits`、`explanationEvidence`
- 重点检查：`rankingDetails[0].contributions` 和 `rerankRuleHits[0].ruleHits` 存在

## 12. 离线评估快照导出
- 调用 `POST /api/admin/evaluation/export?topK=3`
- 期望：返回 `fileName`、`filePath`、`baselineCount`
- 重点检查：目标文件实际存在，且包含 `推荐评估摘要`

## 13. 参数实验矩阵导出
- 调用 `POST /api/admin/evaluation/experiments/export?topKs=3,5`
- 期望：返回 `fileName`、`filePath`、`experimentCount`、`topKValues`
- 重点检查：目标文件实际存在，且包含 `推荐评估矩阵`

## 14. 场景参数矩阵导出
- 调用 `POST /api/admin/evaluation/scenarios/export?scenarioModes=study_partner,interest_partner&topKs=3,5&profileTopTagCounts=3,5&rerankWeightScales=0.8,1.0`
- 期望：返回 `fileName`、`filePath`、`scenarioModes`、`scenarioCount`、`topKValues`、`profileTopTagCounts`、`rerankWeightScales`
- 重点检查：目标文件实际存在，且包含 `推荐场景参数矩阵`

## 15. 透明链路接口
- 调用 `GET /api/admin/demo/pipeline?userId=2001&topK=3&scenarioMode=interest_partner`
- 期望：返回单个用户完整推荐链路
- 重点检查：
  - 存在 `requestUser`
  - 存在 `scenarioStage.objective`
  - 存在 `inputTags`
  - `inputTags[0].tagTypeLabel` 有值
  - 存在 `profileStage`
  - `profileStage.weightFormula` 有值
  - 存在 `recallStage`
  - `recallStage[0].recallFormula` 有值
  - 存在 `rankingStage`
  - `rankingStage[0].rankingFormula` 有值
  - 存在 `rerankStage`
  - `rerankStage[0].ruleDetails` 存在
  - `rerankStage[0].trustBreakdown` 存在
  - 存在 `finalStage`
  - `finalStage` 中可见 `exploration` 与 `explorationReason`

## 16. 透明链路页面
- 访问 `/pipeline.html`
- 期望：页面可打开并展示单个用户的完整推荐链路
- 重点检查：
  - 可切换 `userId`
  - 可切换 `scenarioMode`
  - 页面能显示输入、画像、召回、排序、重排、探索与最终解释

## 17. LLM 解释回退
- 配置 `ZAI_API_KEY` 后调用 `GET /api/recommendations/{recommendationId}/explanation`
- 期望：
  - 始终返回 `reasonText`
  - 始终返回 `reasonSource`
  - 当智谱改写成功时，返回 `ruleReasonText`、`llmReasonText`，且 `reasonSource=llm`
  - 当未配置 key、调用失败或超时时，仍返回规则解释，且 `reasonSource=rule`
- 重点检查：
  - 响应中不应出现无意义的 `:null`
  - 前端解释面板能标识当前是 “LLM 改写解释” 还是 “规则解释”

## 18. 答辩模式与调试模式
- 首页访问 `/`
- 透明链路页访问 `/pipeline.html`
- 期望：
  - 两个页面都提供 “答辩模式” 开关
  - 默认处于答辩模式
  - 答辩模式下隐藏原始 JSON 面板
  - 调试模式下恢复原始 JSON 面板
- 重点检查：
  - 首页解释区同时展示：
    - 当前展示解释
    - 规则依据
    - LLM 改写解释
  - 透明链路页最终阶段可点击 “查看解释对照”
  - 透明链路页会默认加载第一条最终推荐的解释对照
