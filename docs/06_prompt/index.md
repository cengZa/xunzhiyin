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
- `skills/master-thesis-review/SKILL.md`: 中文论文三段式评审 skill，适合对论文内容、规范性、系统实现闭环和答辩评价项做集中审查。本项目按本科工程型论文口径使用其中的审查框架。

## 使用原则
- 先读共享壳，再读角色壳。
- 论文终审或大改后复核时，先读 `skills/master-thesis-review/SKILL.md`，再结合 `docs/05_thesis/` 与生成的 Word 文档审查。
- 壳文件只说明职责、输入、输出、先读哪些文档。
- 细节知识回到 `docs/` 的系统记录层。
