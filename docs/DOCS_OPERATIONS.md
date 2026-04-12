# Docs Operations

本页定义文档体系的最小运维动作。

## 日常动作
- 新增索引时，把它挂到上一级入口。
- 新增计划时，更新 `docs/exec-plans/README.md`。
- 新增角色壳时，更新 `docs/06_prompt/index.md`。
- 重要规则变化时，优先更新 `AGENTS.md` 或 `docs/*.md` 总入口，而不是只改局部 prompt。

## 巡检动作
- 跑统一检查入口：`powershell -ExecutionPolicy Bypass -File scripts/run-governance-checks.ps1`
- 或按需单独运行：
  - `scripts/check-doc-links.ps1`
  - `scripts/check-backend-skeleton.ps1`
  - `scripts/check-module-boundaries.ps1`
  - `scripts/check-service-signatures.ps1`
  - `scripts/check-controller-routes.ps1`
  - `scripts/check-api-contracts.ps1`
  - `scripts/check-java-toolchain.ps1`
- 检查 `docs/design-docs/doc-gardening.md`
- 检查 `docs/exec-plans/tech-debt-tracker.md`
- 检查 `docs/QUALITY_SCORE.md` 是否需要更新

## 当前限制
- 文档检查脚本只覆盖 `.md` 文件引用，不覆盖图片、PDF、代码片段引用。
- 后端骨架检查当前只覆盖文件存在性，不覆盖方法签名和实现质量。
- service 签名检查当前只覆盖核心方法，不检查额外方法与调用语义。
- controller 路由检查当前只覆盖主接口路径，不检查参数、字段与响应语义。
- API contract 检查当前只覆盖关键字段名，不检查运行时序列化结果。
- 模块边界检查当前只覆盖顶层包，不覆盖子包和依赖方向。
- Java toolchain 检查当前只检查 `java` 主版本，不检查 Maven toolchain 细节。
- 检查脚本基于路径模式，不能理解自然语言里的隐式引用。
