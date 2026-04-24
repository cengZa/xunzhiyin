# Metrics Definition

## 当前阶段关注的指标
- `compile_pass`
  - `mvn compile` 是否通过
- `test_pass`
  - `mvn test` 是否通过
- `recall_candidate_count`
  - 推荐请求中的召回候选数
- `topk_return_count`
  - 推荐接口实际返回的项目数量
- `explanation_presence`
  - 每条推荐是否携带 explanation
- `profile_rebuild_success`
  - 反馈后画像是否成功重建
- `precision_at_k_proxy`
  - 以代理相关性规则计算的 `Precision@K`
- `hit_rate_at_k_proxy`
  - Top-K 中至少命中 1 个相关候选的用户占比
- `evaluation_baseline_count`
  - 当前离线评估输出的基线数量，当前应为 `4`
- `evaluation_report_export`
  - 是否可导出 Markdown 格式评估报告
- `evaluation_matrix_export`
  - 是否可导出多组 `topK` 参数实验矩阵
- `scenario_matrix_export`
  - 是否可导出 `scenarioMode + topK + profileTopTagCount + rerankWeightScale` 的场景矩阵

## 当前阶段不追求的指标
- 真实线上 Precision@K
- 大规模性能压测
- 长周期反馈学习效果

## 说明
当前阶段目标是“主链路可运行、可验证、可演示”，不是正式线上实验评测阶段。

## 代理相关性规则
- 当前离线评估不引入真实人工标注
- 使用简化代理规则判断相关性：
  - `shared_tags >= 2`
  - `same_major && shared_tags >= 1`
- 这些规则只用于答辩演示和实验草案，不应被表述为正式学术结论

## 当前评估基线
- `tag_overlap`
  - 按召回分数近似模拟“标签重叠优先”
- `cosine_similarity`
  - 按排序分数模拟“纯相似度排序”
- `full_pipeline_no_trust`
  - 完整链路，但不叠加可信连接分
- `full_pipeline_with_trust`
  - 完整链路，并叠加可信连接分

## 当前实验参数
- `scenarioMode`
- `topK`
- `profileTopTagCount`
- `rerankWeightScale`

## 创新点落地状态
- 已实现：
  - 改进 TF-IDF 画像权重
  - 时间衰减
  - Top-K 画像裁剪
  - 场景化重排
  - 轻量可信连接分
- 已实现：
  - 轻量探索
    - 仅在 `interest_partner` 模式启用
    - 仅在 `topK >= 3` 时启用
    - 保持前 2 名主结果稳定
    - 仅保留 1 个探索位，并放在最后一位

## 轻量探索补充指标
- `exploration_slot_presence`
  - `interest_partner` 模式下 Top-K 是否出现 1 个探索位
- `exploration_reason_presence`
  - 探索位是否返回 `explorationReason`
- `top2_stability`
  - 开启轻量探索后，前 2 名主排序是否保持稳定
