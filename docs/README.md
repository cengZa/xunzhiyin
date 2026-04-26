# Docs README

`docs/` 是本项目的系统记录层。旧的 numbered docs 继续保留内容，新索引层负责让 agent 以渐进披露方式获取信息。

## 阅读入口
- 产品目标与边界：`product-specs/index.md`
- 架构与设计：`design-docs/index.md`
- 计划与任务：`exec-plans/README.md`
- 统一规范：`DESIGN.md`、`PLANS.md`、`PRODUCT_SENSE.md`、`QUALITY_SCORE.md`、`RELIABILITY.md`、`SECURITY.md`
- 文档运维：`DOCS_OPERATIONS.md`
- Prompt 壳：`06_prompt/index.md`
- 论文写作入口：`05_thesis/README.md`

## 旧文档保留原则
- `00_meta`、`01_planning`、`02_design`、`03_backend`、`04_test`、`05_thesis`、`07_task` 仍然有效。
- `05_thesis/` 属于论文输出层，默认不在实现任务的起步阅读链路中；仅在论文写作、答辩材料整理、图表说明编写时进入。
- 以后新增 agent 可读知识时，优先落到新的索引层，再决定是否沉到旧分区。

## 文档治理规则
- 重要规则先写成短索引，再链接到细节文档。
- 执行计划必须放进 `exec-plans/`。
- 跨文档冲突需要在计划或质量文档里显式记录。

## 当前收口入口
- 测试与验收：`04_test/README.md`
- 最终验收矩阵：`04_test/acceptance_traceability_matrix.md`
- 答辩演示脚本：`04_test/final_demo_script.md`
- 截图与接口证据：`04_test/demo_evidence_samples.md`
- 创新点与重难点：`05_thesis/innovation_and_difficulty.md`
- 本地参考文献资料：docs/参考文献 目录下的 README 页面
- 工程型论文与答辩参考：docs/更多参考 目录下的 README 页面
