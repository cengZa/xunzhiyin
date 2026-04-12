# Result Analysis

## 当前结论
- 项目已经具备可复用的离线评估摘要能力，可以输出 3 组推荐基线对比
- 当前结果更适合答辩演示和实验草案，不适合作为真实线上效果结论
- 评估入口已经接入管理接口，既可返回结构化摘要，也可导出 Markdown 报告与参数实验矩阵

## 本阶段输出
- `GET /api/admin/evaluation/summary?topK=3`
- `GET /api/admin/evaluation/report?topK=3`
- `POST /api/admin/evaluation/export?topK=3`
- `POST /api/admin/evaluation/experiments/export?topKs=3,5`

## 当前实验观察
- 在当前 12 用户 / 12 标签 / 48 关系的 mock 数据下，`topK=3` 的代理 `Precision@K` 高于 `topK=5`
- 这说明在小规模校园兴趣样本里，返回更短的候选列表更容易保持“相关性密度”
- 当前数据下三组基线差异不大，说明 mock 数据仍偏规则化，后续需要继续拉大用户分布差异

## 如何解读指标
- `averageRecallCandidateCount`: 每个请求用户平均召回到多少候选
- `averageTopKReturnCount`: 每个请求用户最终参与评估的 Top-K 平均条数
- `Precision@K`: Top-K 中被代理规则判定为相关的占比
- `HitRate@K`: 至少命中 1 个相关候选的请求用户占比
- `ExplanationPresenceRate`: Top-K 中能生成非空解释文本的占比

## 风险与限制
- 相关性来自代理规则，不是人工标注
- mock 数据规模仍较小，指标稳定性有限
- 当前评估用于比较参数和基线差异，不用于证明模型在真实校园场景的泛化能力

## 下一步
- 增加更多 `topK` 取值并保留多次矩阵快照
- 继续把参数实验扩展到画像 Top 标签数量和重排权重
- 在此基础上整理论文表格与答辩材料
