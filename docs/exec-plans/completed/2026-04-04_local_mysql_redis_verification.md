# 2026-04-04 Local MySQL Redis Verification

## 背景
当前项目已经在 Java 21 + H2 环境下通过启动级集成测试，但真实本地依赖环境仍未完成自动化联调。用户明确允许优先使用本机 MySQL 和 Redis。

## 目标
- 用本地 MySQL 完成建库、建表与 mock 数据初始化验证
- 评估并验证本地 Redis 是否可用于倒排索引与画像缓存
- 将本地联调所需配置、脚本和验证步骤写回仓库

## 当前状态
- 当时已新增 `application-local.yml` 支持本地 MySQL / Redis；现已并回单一 `application.yml`
- 已补可执行的 `docs/03_backend/schema.sql` 与运行时 `src/main/resources/schema-local.sql`
- 已新增 `scripts/run-local-env-verification.ps1`
- 已新增 `LocalMysqlFlowIntegrationTest` 并在本机 `MySQL + Redis` 上通过
- `LocalMysqlFlowIntegrationTest` 默认在全量 `mvn test` 中跳过，仅在显式传入 `-Dlocal.integration.enabled=true` 时执行

## 范围内
- 修复可执行的 MySQL schema/init 脚本
- 新增本地 profile 与本地联调脚本
- 验证应用在本地 MySQL 上的核心链路
- 更新计划、测试与债务文档

## 范围外
- Docker / Testcontainers
- 生产部署配置
- 推荐算法本身优化

## 输入文档
- `docs/02_design/database_design.md`
- `docs/02_design/redis_design.md`
- `docs/03_backend/schema.sql`
- `docs/04_test/test_cases.md`
- `docs/exec-plans/completed/2026-04-03_integration_verification.md`

## 执行步骤
1. 检查本机 MySQL 与 Redis 连通性，确认当前阻塞点。
2. 修复并拆分可执行的 MySQL schema/init 脚本，新增本地 profile。
3. 在本地 MySQL 上完成建库建表，并验证 mock 数据初始化。
4. 验证 Redis 是否可用；若不可用，明确记录阻塞与降级路径。
5. 更新测试文档、计划状态与技术债记录。

## 风险
- 现有 `schema.sql` 含编码和 Markdown 混杂问题，无法直接执行。
- 本地 Redis 端口当前未监听，可能需要用户启动服务或提供真实连接信息。
- 应用默认配置与本地实际账号密码可能不一致。

## 验收标准
- 仓库内存在可直接执行的本地 MySQL 初始化脚本。
- 本地 profile 能明确指向真实 MySQL / Redis 配置。
- 至少完成一次本地 MySQL 上的建库建表与 mock 数据初始化验证。
- 若 Redis 不可用，阻塞原因和后续动作被明确记录。

## 决策日志
- 2026-04-04：优先使用用户本地 MySQL/Redis，而不是继续先做容器化环境。
- 2026-04-04：本机 MySQL 默认凭据不是 `root/123456`，因此改为环境变量覆盖。
- 2026-04-04：本机 Redis Server 位于 `D:\Redis\Redis-x64-5.0.14.1\redis-server.exe`，已按本地配置启动并验证 `PING` 成功。
- 2026-04-04：为避免把全量测试绑死到个人机器，本地联调测试保留显式开关，不默认纳入 `mvn test`。
