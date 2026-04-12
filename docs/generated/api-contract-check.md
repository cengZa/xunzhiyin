# API Contract Check

此检查对应 `docs/02_design/api_design.md` 的关键入参与关键返回字段。

## 脚本
- `scripts/check-api-contracts.ps1`

## 当前覆盖
- 用户创建请求字段
- 用户标签绑定请求字段
- 推荐请求参数 `topK` / `useCache`
- 反馈提交请求字段
- 画像、推荐详情、单条解释的关键返回字段
- `userId` 与 `profileUpdated` 这类直接返回标记

## 规则
- 文档要求的关键字段必须在 DTO / VO / controller 中出现。
- 现阶段允许存在额外字段，不视为失败。

## 当前限制
- 只按文本检查字段名和标记，不做 Java AST 解析。
- 不检查字段类型是否百分百匹配文档。
- 不检查运行时序列化结果。
