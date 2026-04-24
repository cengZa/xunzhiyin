# 前端演示页设计说明

## 目标
- 提供一个与后端同源的静态演示页，不引入额外前端构建链路。
- 页面风格从“工程控制台”收紧为“产品首页 + 演示侧栏”。
- 面向答辩展示，不追求完整社交产品体验。

## 入口
- 页面路由：`/`
- 静态资源：
  - `src/main/resources/static/index.html`
  - `src/main/resources/static/app.css`
  - `src/main/resources/static/app.js`

## 当前信息结构
- Hero 区：
  - 项目定位
  - 演示用户
  - 当前场景模式
  - 算法亮点
- 答辩故事线：
  - 固定演示用户 `2001`
  - 支持 `学习搭子 / 社团搭子 / 兴趣同频` 三种模式切换
  - 展示模式对应的人设和重点候选人
- 用户画像区：
  - Top 标签
  - 画像原始数据
- 离线评估区：
  - 当前摘要
  - 基线对比
  - 实验结论
- 双视图对比区：
  - `标签重叠基线`
  - `完整链路`
  - 同屏展示排序差异
- 推荐结果区：
  - 推荐卡片
  - 分数拆解
    - 兴趣分
    - 场景分
    - 可信分
    - 最终分
  - 匹配标签
  - 命中规则
  - 可信原因
- 推荐解释区：
  - 人类可读摘要
  - 原始解释折叠区
- 反馈前后变化区：
  - 一次 `follow` 后的画像变化
  - 推荐变化
  - 解释变化
- 演示侧栏：
  - 初始化数据
  - 重建画像
  - 重建召回
  - 导出评估快照
  - 导出参数矩阵
  - 导出演示场景矩阵
  - 日志输出

## 页面约束
- 单页、静态资源、无独立前端构建系统。
- 直接复用现有后端接口。
- 默认展示人能看懂的摘要，不默认堆原始 JSON。
- 原始 JSON 保留在折叠区，作为调试和答辩备选材料。

## 与后端契约
- 故事线接口：`GET /api/admin/demo/story?scenarioMode=...`
- 双视图接口：`GET /api/admin/demo/compare?userId=2001&topK=3&scenarioMode=...`
- 推荐接口：`GET /api/recommendations/{userId}?topK=3&useCache=false&scenarioMode=...`
- 场景矩阵接口：`POST /api/admin/evaluation/scenarios/export`

首页会直接消费这些字段：
- `scenarioMode`
- `scenarioLabel`
- `interestScore`
- `campusScore`
- `trustScore`
- `matchedTags`
- `matchedRules`
- `trustReasons`
- `recommendationLabel`

## 展示叙事
- 页面围绕“兴趣相似 + 校园场景重排 + 可信连接分”的新算法匹配组织。
- 重点讲清楚：
  - 为什么完整链路比仅标签重叠更合理
  - 为什么同一用户在不同场景模式下推荐结果不同
  - 为什么 `follow` 后推荐会变化
  - 为什么这套输出适合作为答辩演示和论文素材
