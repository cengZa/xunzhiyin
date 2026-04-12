# Doc Source Map

## 目标
把现有 numbered docs 重新挂载到 harness 风格入口层，避免重复造轮子。

## 映射关系
- `00_meta/` -> 产品背景、术语、约束基础
- `01_planning/` -> 产品规格与验收依据
- `02_design/` -> 架构与设计事实来源
- `03_backend/` -> 后端实现设计事实来源
- `04_test/` -> 测试与评估依据
- `05_thesis/` -> 论文写作输出层
- `06_prompt/` -> agent shells
- `07_task/` -> 历史任务输入，后续逐步迁入 `exec-plans/`

## 当前策略
- 保留原文件路径，减少断链。
- 用新索引层建立导航。
- 后续迁移只在确有收益时进行。
