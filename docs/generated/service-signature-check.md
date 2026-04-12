# Service Signature Check

此检查对应 `docs/03_backend/service_design.md` 的核心方法签名。

## 脚本
- `scripts/check-service-signatures.ps1`

## 当前覆盖
- `UserService`
- `TagService`
- `ProfileService`
- `RecallService`
- `RankingService`
- `RerankService`
- `ExplanationService`
- `FeedbackService`
- `RecommendationService`

## 规则
- 文档要求的核心方法必须存在。
- 现阶段允许接口中存在额外方法，不视为失败。

## 当前限制
- 只按文本检查签名，不解析 AST。
- 不检查实现类是否完整实现文档语义。
- 不检查 service 间调用关系。
