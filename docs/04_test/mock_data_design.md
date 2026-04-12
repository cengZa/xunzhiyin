# Mock Data Design

## 1. 目标
为推荐主链路提供可重复初始化的演示数据，支持画像构建、召回、排序、重排、解释、反馈和离线评估全链路调试。

## 2. 初始化入口
- `POST /api/admin/mock/init`
- `POST /api/admin/profiles/rebuild-all`
- `POST /api/admin/recall/rebuild-index`

## 3. 当前 mock 数据范围
### 用户
- 12 个用户
- 覆盖 2021 / 2022 / 2023 / 2024 年级
- 覆盖 Computer Science / Software Engineering / Design / Mathematics / Music / Data Science / Journalism / Finance / Architecture 等专业

### 标签
- 12 个标签
- academic: Java, Spring, AI, Frontend
- hobby: Music, Running, Photography, Hiking
- club: Basketball, ACM, Volunteering
- interest: Startup

### 用户标签关系
- 共 48 条关系
- 每个用户 4 个标签
- 同时覆盖技术、文艺、运动、志愿、创业等兴趣圈层

## 4. 初始化后可验证的现象
- 用户画像可生成更稳定的 Top-K 标签
- 多个兴趣圈层会形成可解释的召回候选
- 技术向用户更容易互相推荐
- 摄影、音乐、志愿等跨专业兴趣会产生跨学院召回
- 反馈后用户画像会重建
- 离线评估摘要可基于这批数据输出 3 组基线对比

## 5. 与评估导出的关系
- 评估摘要接口：`GET /api/admin/evaluation/summary`
- 评估报告接口：`GET /api/admin/evaluation/report`
- 评估落盘接口：`POST /api/admin/evaluation/export`
- 默认导出路径：`docs/generated/recommendation-evaluation-latest.md`
- 集成测试导出路径：`target/generated-docs/recommendation-evaluation-latest.md`

## 6. 当前限制
- 仍为小规模演示数据，不用于正式学术指标结论
- 倒排索引以 Redis 为优先，Redis 不可用时主链路会降级到 DB 标签关系查询
- 集成测试默认使用 H2 内存数据库验证接口闭环，不覆盖真实 MySQL 方言差异
