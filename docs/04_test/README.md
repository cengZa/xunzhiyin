# Test And Acceptance Index

`04_test/` 用于承接测试用例、验证口径、结果分析与答辩演示路径。

## 建议阅读顺序
- `test_cases.md`: 功能、接口、页面、评估与解释回退测试清单
- `acceptance_traceability_matrix.md`: 需求、验收、代码、测试与论文章节映射
- `demo_presentation_guide.md`: 录制演示视频的固定顺序、操作说明与对应讲稿
- `defense_question_bank.md`: 答辩老师可能提问与建议回答口径
- `final_demo_script.md`: 答辩演示固定路径
- `demo_evidence_samples.md`: 论文和答辩截图、接口样例与图表建议
- `metrics_definition.md`: 指标定义、代理验证边界与当前实验参数
- `opening_report_validation_map.md`: 开题报告量化目标与当前证据缺口
- `result_analysis.md`: 当前结果结论、限制与论文表述边界
- `../generated/final-validation-latest.md`: 最近一次治理检查与 Maven 测试结果

## 使用约束
- 使用 mock 数据、代理相关性规则或离线快照时，论文中必须显式标注。
- 未被代码、测试或生成结果支撑的结论，不写成既成事实。
- 答辩演示优先使用 `userId=2001`、`topK=3` 和三类 `scenarioMode`。
