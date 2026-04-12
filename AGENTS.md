# AGENTS.md

本仓库采用 harness engineering 风格组织：`AGENTS.md` 只做入口导航，`docs/` 才是系统记录。

## 1. 工作原则
- 人类定义目标、边界、验收标准与优先级。
- Agent 直接在仓库内读文档、改文件、验证结果。
- 先补环境、约束与反馈回路，再追求更高吞吐。
- 优先编码可验证的不变量，而不是堆叠冗长提示词。
- 文档失真时，以修正文档或增加机械约束为优先动作。

## 2. 起步阅读顺序
1. `docs/README.md`
2. `ARCHITECTURE.md`
3. 与任务最相关的领域入口：`docs/DESIGN.md`、`docs/PLANS.md`、`docs/PRODUCT_SENSE.md`
4. 对应专项文档与执行计划

## 3. 系统记录位置
- 产品边界与目标：`docs/product-specs/`
- 设计与架构：`docs/design-docs/`
- 执行计划与债务：`docs/exec-plans/`
- 引用材料：`docs/references/`
- 生成型资料与派生物：`docs/generated/`
- 统一规范页：`docs/DESIGN.md`、`docs/PLANS.md`、`docs/QUALITY_SCORE.md`、`docs/RELIABILITY.md`、`docs/SECURITY.md`

## 4. Agent Shell
- Prompt 入口：`docs/06_prompt/index.md`
- 共享壳：`docs/06_prompt/_global_agent_rules.md`
- 角色壳：`docs/06_prompt/*_prompt.md`
- 所有壳文件都应保持短、小、可导航，不重复系统记录内容。

## 5. 工作流
1. 读取任务相关索引和专项文档。
2. 在 `docs/exec-plans/active/` 检查是否已有执行计划；复杂任务先补计划。
3. 实施改动时，优先保持现有结构稳定，避免无索引的大规模搬迁。
4. 完成后更新相关文档、计划状态、质量或债务记录。
5. 若发现同类错误反复出现，应优先把规则编码到文档、脚本或检查项里。

## 6. 文档纪律
- `AGENTS.md` 不承载百科全书式知识。
- 每个索引文档都要给出“该读什么、何时读、哪些旧文档仍有效”。
- 旧 numbered docs 允许保留，但必须被新的索引层重新挂载。
- 新增规范优先写入 `docs/`，避免只停留在聊天记录。

## 7. 任务完成定义
- 文件改动与目标一致。
- 相关索引可导航到真实内容。
- 若无法验证，明确说明缺口和阻塞项。
- 输出中列出新建/修改文件及其作用。
