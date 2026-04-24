# Docs Operations

本页定义文档体系的最小运维动作。

## 日常动作
- 新增索引时，把它挂到上一级入口。
- 新增计划时，更新 `docs/exec-plans/README.md`。
- 新增角色壳时，更新 `docs/06_prompt/index.md`。
- 重要规则变化时，优先更新 `AGENTS.md` 或 `docs/*.md` 总入口，而不是只改局部 prompt。

## 巡检入口
- 跑统一检查入口：`powershell -ExecutionPolicy Bypass -File scripts/run-governance-checks.ps1`
- 若需了解各检查项覆盖范围、限制和后续自动化方向，查看 `docs/design-docs/doc-gardening.md`

## 巡检后动作
- 检查 `docs/exec-plans/tech-debt-tracker.md`
- 检查 `docs/QUALITY_SCORE.md` 是否需要更新
- 若发现新规则，优先沉淀到索引、规范页或脚本，而不是只写在对话里
