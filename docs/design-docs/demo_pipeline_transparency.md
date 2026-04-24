# Demo Pipeline Transparency

## 目标
- 新增一个独立的透明链路页，而不是把首页改成调试台。
- 透明链路能力对任意用户开放。
- `2001` 仅作为默认答辩用户，不作为技术硬编码目标。

## 后端接口
- `GET /api/admin/demo/pipeline?userId=2001&topK=3&scenarioMode=interest_partner`

## 返回结构
- `requestUser`
- `scenarioStage`
- `inputTags`
- `profileStage`
- `recallStage`
- `rankingStage`
- `rerankStage`
- `finalStage`

## 关键解释字段
- `scenarioStage`
  - 当前模式目标
  - 当前模式会如何调整规则权重
- `inputTags`
  - 每个标签的领域分类，如学术 / 爱好 / 社团 / 兴趣
- `profileStage`
  - `weightFormula`
  - 每个标签的 `tf / idf / timeDecay / finalWeight`
- `recallStage`
  - `recallFormula`
  - `matchedRecallTags`
  - `recallTrace`
- `rankingStage`
  - `rankingFormula`
  - `rankingDetail.dotProduct / requestNorm / candidateNorm`
- `rerankStage`
  - `ruleDetails`
  - `trustBreakdown`
  - `finalScoreFormulaLabel`

## 前端页面
- 路径：`/pipeline.html`
- 展示顺序：
  1. 输入标签
  2. 画像构建
  3. 召回候选池
  4. 排序结果
  5. 重排 / 可信连接分 / 轻量探索
  6. 最终 Top-K 与解释
  7. 原始 JSON 调试区

## 设计原则
- 首页负责展示结果、亮点和答辩叙事。
- 透明链路页负责展示推荐过程和工程实现。
- 不让前端自己拼多份接口，而是由后端提供聚合结果。
