# Thesis Docs

`05_thesis/` 用于承接论文写作输出，不是实现任务的默认知识入口。

## 何时进入
- 编写论文章节、摘要、结论
- 整理答辩材料中的系统说明、实验描述、图表说明
- 校对论文文本与当前实现是否一致

## 建议阅读顺序
1. `thesis_outline.md`
2. `template_bridge.md`
3. 与当前写作任务对应的章节文件
4. `../00_meta/source_opening_report.md`
5. `../04_test/` 中相关测试与评估文档
6. `../generated/` 中最新评估快照或实验矩阵

## 使用约束
- 论文表述必须回链到仓库内已实现、已验证的事实。
- 未完成或未验证的能力，不写成既成事实。
- 若实现已变化，先更新系统记录，再回写论文文本。

## 方便 agent 的工具
- 读取学校模板、开题报告或批注稿时，可运行：
  - `powershell -ExecutionPolicy Bypass -File scripts/extract-docx-text.ps1 -InputPath <docx路径>`
- 若要将抽取文本落盘，增加：
  - `-OutputPath <输出txt或md路径>`
