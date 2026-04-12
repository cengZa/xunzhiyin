# Prompt Shell Index

`06_prompt/` 现在只保存 agent shells，不再承载全部项目知识。

## 组成
- `_global_agent_rules.md`: 全角色共享壳
- `shell-template.md`: 新角色壳模板
- `master_prompt.md`: 总调度壳
- `planner_prompt.md`
- `architect_prompt.md`
- `algorithm_engineer_prompt.md`
- `backend_engineer_prompt.md`
- `academic_writer_prompt.md`
- `reviewer_prompt.md`

## 使用原则
- 先读共享壳，再读角色壳。
- 壳文件只说明职责、输入、输出、先读哪些文档。
- 细节知识回到 `docs/` 的系统记录层。
