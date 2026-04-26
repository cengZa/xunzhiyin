# Acceptance Traceability Matrix

本页用于把功能需求、验收标准、实现位置、测试材料和论文证据连接起来。状态分为：

- `已实现`: 代码已有对应能力。
- `已验证`: 已有自动化测试、治理检查或可重复接口验证。
- `代理验证`: 使用 mock 数据、代理相关性规则或离线快照证明链路合理性。
- `待补证据`: 机制存在，但缺少正式统计、截图、人工标注或问卷材料。

## 1. 功能需求映射

| 需求 | 当前状态 | 主要实现位置 | 验证材料 | 论文位置 |
| --- | --- | --- | --- | --- |
| FR-01 用户管理 | 已验证 | `controller/UserController.java`, `service/UserService.java` | `test_cases.md` 2,3,5 | `../05_thesis/chapter3_analysis.md`, `../05_thesis/chapter5_implementation.md` |
| FR-02 标签管理 | 已验证 | `controller/TagController.java`, `service/TagService.java` | `test_cases.md` 3,5,6 | `../05_thesis/chapter3_analysis.md`, `../05_thesis/chapter5_implementation.md` |
| FR-03 用户画像生成 | 已验证 | `service/ProfileService.java`, `strategy/profile/` | `ProfileServiceImplTest`, `test_cases.md` 6 | `../05_thesis/chapter5_implementation.md` |
| FR-04 改进 TF-IDF 权重计算 | 已验证 | `ImprovedTfIdfProfileWeightCalculator.java` | `ProfileServiceImplTest`, `opening_report_validation_map.md` | `../05_thesis/chapter5_implementation.md` |
| FR-05 倒排召回 | 已验证 | `service/RecallService.java`, `infra/redis/RecallIndexRepository.java` | `ApiFlowIntegrationTest`, `test_cases.md` 7 | `../05_thesis/chapter5_implementation.md` |
| FR-06 相似度排序 | 已验证 | `service/RankingService.java` | `RecommendationServiceImplTest`, `test_cases.md` 7,11.1 | `../05_thesis/chapter5_implementation.md` |
| FR-07 规则重排 | 已验证 | `service/RerankService.java`, `strategy/rerank/` | `RerankServiceImplTest`, `test_cases.md` 7,11.1 | `../05_thesis/chapter5_implementation.md` |
| FR-08 推荐结果输出 | 已验证 | `controller/RecommendationController.java`, `service/RecommendationService.java` | `ApiFlowIntegrationTest`, `test_cases.md` 7 | `../05_thesis/chapter5_implementation.md`, `../05_thesis/chapter6_test.md` |
| FR-09 解释生成 | 已验证 | `service/ExplanationService.java`, `strategy/explain/` | `ExplanationServiceImplTest`, `test_cases.md` 17 | `../05_thesis/chapter5_implementation.md` |
| FR-10 反馈采集 | 已验证 | `controller/FeedbackController.java`, `service/FeedbackService.java` | `ApiFlowIntegrationTest`, `test_cases.md` 8 | `../05_thesis/chapter5_implementation.md`, `../05_thesis/chapter6_test.md` |
| FR-11 反馈更新 | 已验证 | `FeedbackServiceImpl.java`, `ProfileServiceImpl.java` | `ApiFlowIntegrationTest`, `test_cases.md` 8,8.1 | `../05_thesis/chapter5_implementation.md`, `../05_thesis/chapter6_test.md` |
| FR-12 推荐记录持久化 | 已验证 | `mapper/RecommendationResultMapper.java`, `mapper/RecommendationExplanationMapper.java` | `ApiFlowIntegrationTest`, `test_cases.md` 7,11.1 | `../05_thesis/chapter5_implementation.md`, `../05_thesis/chapter6_test.md` |

## 2. 验收标准映射

| 验收项 | 当前状态 | 证据 | 论文表述边界 |
| --- | --- | --- | --- |
| AC-01 用户与标签基础能力 | 已验证 | `test_cases.md` 3,5,6 | 可写为已完成 |
| AC-02 画像生成 | 已验证 | `ProfileServiceImplTest`, `test_cases.md` 6 | 可写为已完成 |
| AC-03 候选召回 | 已验证 | `ApiFlowIntegrationTest`, `test_cases.md` 7 | 可写为已完成 |
| AC-04 排序与重排 | 已验证 | `RecommendationServiceImplTest`, `RerankServiceImplTest` | 可写为已完成 |
| AC-05 推荐解释 | 已验证 | `ExplanationServiceImplTest`, `test_cases.md` 17 | 可写为已完成；LLM 只写解释改写增强 |
| AC-06 反馈更新 | 已验证 | `ApiFlowIntegrationTest`, `test_cases.md` 8,8.1 | 可写为已完成 |
| AC-07 工程结构 | 已验证 | `scripts/check-backend-skeleton.ps1`, `scripts/check-module-boundaries.ps1` | 可写为已完成 |
| AC-08 数据设计 | 已验证 | `docs/02_design/database_design.md`, `src/main/resources/schema-local.sql` | 可写为已完成 |
| AC-09 一致性 | 代理验证 | `acceptance_traceability_matrix.md`, `../05_thesis/thesis_outline.md` | 可写为已建立一致性映射 |
| AC-10 数据与指标规范 | 代理验证 | `metrics_definition.md`, `opening_report_validation_map.md` | 必须说明 mock 数据与代理规则 |
| AC-11 可演示性 | 已验证 | `final_demo_script.md`, `test_cases.md` 1.1,15,16,18 | 可写为已完成 |
| AC-12 文档完整性 | 已验证 | `../README.md`, `../DESIGN.md`, `../05_thesis/README.md` | 可写为已建立系统记录 |
| AC-13 画像量化目标 | 待补证据 | `opening_report_validation_map.md` | 不写成完全达标 |
| AC-14 召回与排序量化目标 | 待补证据 | `generated/recommendation-evaluation-latest.md`, `opening_report_validation_map.md` | 只写代理验证结果 |
| AC-15 解释与反馈量化目标 | 待补证据 | `ExplanationServiceImplTest`, `opening_report_validation_map.md` | 不写问卷或真实提升结论 |
| AC-16 代理验证边界 | 已验证 | `metrics_definition.md`, `result_analysis.md` | 必须写入论文第 7 章 |

## 3. 当前最短结论

项目功能链路、演示链路和自动化验证链路已具备，可作为工程型毕设主体。量化目标中涉及真实人工标注、问卷满意度和多轮反馈收益的部分，当前只能作为代理验证或后续工作，不能在论文中写成正式用户研究结论。

