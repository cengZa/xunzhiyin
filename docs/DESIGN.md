# DESIGN

本页是设计总入口，避免 agent 在 `02_design/` 与 `03_backend/` 中盲搜。

## 先读这些
- 系统架构：`02_design/system_architecture.md`
- 模块设计：`02_design/module_design.md`
- 推荐管线：`02_design/recommendation_pipeline.md`
- API：`02_design/api_design.md`
- 数据库：`02_design/database_design.md`
- Redis：`02_design/redis_design.md`
- 后端结构：`03_backend/backend_structure.md`
- 服务设计：`03_backend/service_design.md`

## 设计不变量
- 先保证主链路清晰，再补优化。
- 解释必须来自排序/重排证据，不允许独立编造。
- 不引入 docs 之外的大模块。
- 面向本科毕设，偏工程实现而非研究平台。
