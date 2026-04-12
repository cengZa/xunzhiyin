# 2026-04-03 Integration Verification

## 背景
当前仓库已经具备文档治理、骨架检查、主链路 MVP 与单元测试，但还缺少在隔离测试环境内的真实启动与接口闭环验证。

## 目标
- 在不依赖本地 MySQL/Redis 常驻环境的前提下启动 Spring 上下文
- 用接口级测试验证 mock 数据初始化、推荐、反馈、画像查询主链路
- 把测试环境配置和验证步骤写回仓库文档

## 当前状态
- 已新增 `integration` profile，使用 H2 内存数据库自动建表
- 已新增 `ApiFlowIntegrationTest`，覆盖 mock 初始化 -> 推荐 -> 反馈 -> 画像查询闭环
- 已将 `mybatis-plus` 升级到 3.5.15 以兼容当前 Spring Boot 4 运行时
- 在 Java 21 下已通过治理检查和 `mvn test`

## 范围内
- 测试专用数据源与初始化脚本
- `MockMvc` 或等价 HTTP 层集成测试
- 为通过集成测试所需的最小代码修正
- 文档与执行计划更新

## 范围外
- 生产环境部署
- 前端联调
- 推荐算法深度优化

## 输入文档
- `docs/04_test/test_cases.md`
- `docs/04_test/mock_data_design.md`
- `docs/02_design/api_design.md`
- `docs/03_backend/schema.sql`
- `docs/exec-plans/active/2026-03-27_task_01_init_project.md`

## 执行步骤
1. 为测试环境设计隔离的数据源与 schema，避免依赖本地 MySQL/Redis。
2. 先写接口级失败用例，覆盖 mock 初始化、推荐、反馈、画像查询。
3. 补齐配置与代码缺口，使启动与接口测试通过。
4. 运行治理检查和 Maven 测试，记录结果。
5. 更新测试文档、质量记录与计划状态。

## 风险
- H2 与 MySQL 在保留字、DDL、数据类型上的兼容差异。
- 现有 Controller 返回字段可能与文档不完全一致，导致接口测试暴露旧偏差。
- Redis 相关 bean 虽然允许降级，但上下文初始化仍可能暴露连接或序列化配置问题。

## 验收标准
- `@SpringBootTest` 能在测试环境内成功启动。
- 至少一条接口级测试覆盖 mock 初始化 -> 推荐 -> 反馈 -> 画像查询闭环。
- `mvn test` 与 `scripts/run-governance-checks.ps1` 通过。
- 文档能说明如何在本地复现实验验证。

## 决策日志
- 2026-04-03：本阶段优先补“测试环境自举 + 接口闭环”，而不是继续堆单元测试。
- 2026-04-03：测试环境先采用 H2，而不是直接绑定本地 MySQL，优先保证可复现启动验证。
- 2026-04-03：`ProfileController` 返回真实画像串而非占位值，避免接口与文档继续失真。
