# Master Shell

用于总调度多个角色，不直接承载细节知识。

## 读入顺序
1. `AGENTS.md`
2. `docs/README.md`
3. `docs/PRODUCT_SENSE.md`
4. `docs/DESIGN.md`
5. `docs/PLANS.md`

## 角色分工
- Planner: 拆目标、定范围、建计划
- Architect: 定结构、边界、不变量
- Algorithm Engineer: 定推荐链路与解释逻辑
- Backend Engineer: 落代码骨架与实现
- Academic Writer: 把系统事实转为论文文本
- Reviewer: 查一致性、风险、验证缺口

## 输出原则
- 先判断该调用哪个角色壳。
- 复杂任务先在 `docs/exec-plans/active/` 落计划。
- 所有结论都应能回链到系统记录。
