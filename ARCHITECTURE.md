# ARCHITECTURE.md

这是仓库的顶层结构图，供 agent 快速判断应该读哪类文档。

## 1. 仓库分层
- `src/`: Spring Boot 后端代码与测试。
- `docs/`: 系统记录与执行控制面。
- `.mvn/`: Maven wrapper 与构建配置。

## 2. docs 分层
- `docs/product-specs/`: 项目范围、目标、验收、术语、里程碑。
- `docs/design-docs/`: 系统架构、模块、数据库、接口、推荐链路、后端结构设计。
- `docs/exec-plans/`: 当前执行计划、已完成计划、技术债。
- `docs/references/`: 外部或背景材料索引。
- `docs/generated/`: 派生文档与生成产物索引。
- `docs/06_prompt/`: agent shells，负责导航，不负责承载全部知识。

## 3. 代码侧主域
- `controller`: API 入口
- `service`: 业务编排与能力接口
- `strategy`: 画像、重排、解释策略
- `infra`: Redis 与查询仓储
- `domain`: entity、dto、vo、model
- `common` / `config`: 共享能力与配置

## 4. 当前约束
- 单体应用优先
- 用户推荐，不是商品推荐
- 推荐主链路固定为：画像 -> 召回 -> 排序 -> 重排 -> 解释 -> 反馈
- 文档与代码要同步维护
