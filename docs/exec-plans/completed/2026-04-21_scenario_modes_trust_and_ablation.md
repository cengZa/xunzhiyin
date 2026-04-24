# 2026-04-21 场景模式、可信连接分与消融实验补强

## 目标
- 在既有推荐主链路上加入三种校园场景模式：
  - `study_partner`
  - `club_partner`
  - `interest_partner`
- 增加轻量可信连接分，不新增数据库表。
- 把实验从单一 `topK` 推进到“场景模式 + 参数矩阵 + 消融对比”。
- 让首页展示与答辩叙事同步更新。

## 已完成
- 新增场景模式常量与标签映射。
- `RecommendationTuningContext` 扩展为统一承载：
  - `profileTopTagLimit`
  - `rerankWeightScale`
  - `scenarioMode`
  - `trustEnabled`
- 推荐主链路支持 `scenarioMode` 参数透传。
- 重排层新增场景权重差异与可信连接分加权。
- 解释证据新增：
  - `scenarioMode`
  - `scenarioLabel`
  - `interestScore`
  - `campusScore`
  - `trustScore`
  - `trustReasons`
- 首页和演示侧栏新增：
  - 场景切换
  - 可信原因展示
  - 场景参数矩阵导出
- 评估摘要新增基线：
  - `tag_overlap`
  - `cosine_similarity`
  - `full_pipeline_no_trust`
  - `full_pipeline_with_trust`

## 验证
- `mvn -gs .mvn\\temp-settings.xml -Dmaven.repo.local=D:\\.projects\\xunzhiyin\\.m2repo -Dsurefire.useFile=false test`
  - 结果：`Tests run: 18, Failures: 0, Errors: 0, Skipped: 1`
- `powershell -ExecutionPolicy Bypass -File scripts\\run-governance-checks.ps1`
  - 结果：全部通过

## 结果
- 推荐系统从“兴趣相似 + 校园规则增强”推进到“兴趣相似 + 场景模式 + 可信连接”的可解释多目标匹配。
- 首页、接口、实验与测试已经与这一叙事对齐。
- 这一轮没有扩表，没有引入超出毕设边界的重型能力。

## 后续建议
- 继续做答辩模式收口。
- 补论文与 PPT 口径中的实验结论表。
- 若继续提升推荐质量，优先考虑时间衰减与轻量探索，而不是重型模型。
