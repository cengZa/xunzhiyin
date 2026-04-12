# Redis Design
# Redis 设计

## 1. 设计目标

Redis 在本项目中主要承担两个职责：
1. 支撑倒排召回
2. 作为推荐链路中的高频读取缓存

## 2. Key 设计

### 2.1 倒排索引

#### recall:inv:tag:{tagId}
类型：Set
含义：拥有该标签的用户 ID 集合

用途：
- 召回阶段根据目标用户 Top-K 标签取并集或加权并集
- 快速得到候选用户集合

示例：
recall:inv:tag:1001 -> {12, 18, 25, 40}

### 2.2 用户画像缓存

#### profile:user:{userId}
类型：String / Hash
含义：用户画像缓存

建议内容：
- profile_version
- vector_json
- topk_json
- updated_at

用途：
- 减少反复读取 MySQL user_profile 表
- 支撑推荐主链路快速获取画像

### 2.3 推荐结果缓存

#### recommend:topk:{userId}
类型：List / ZSet
含义：目标用户最近一次 Top-K 推荐结果缓存

用途：
- 提升推荐结果重复查询性能
- 支持前端重复打开推荐列表时快速返回

### 2.4 候选集缓存（可选）

#### recommend:candidates:{userId}
类型：Set / ZSet
含义：最近一次候选用户集合

用途：
- 调试召回结果
- 用于论文展示召回阶段结果

## 3. 召回策略建议

1. 目标用户取 Top-K 核心标签
2. 逐个读取 recall:inv:tag:{tagId}
3. 做集合并集，生成候选池
4. 去重、过滤自己、过滤无效用户
5. 若候选过多，可按公共标签数做一次粗排

## 4. 更新策略

### 4.1 用户标签变更时
- 更新 user_tag_relation
- 重建用户画像
- 更新 profile:user:{userId}
- 维护对应标签的倒排集合

### 4.2 反馈更新时
- 调整 user_profile
- 同步刷新 profile:user:{userId}
- 若 Top-K 标签变化明显，则更新倒排索引映射

## 5. 设计注意事项

1. Redis 只承担高频读与倒排，不作为唯一事实来源
2. 最终一致性以 MySQL 为准
3. 倒排集合更新应封装成统一方法，避免漏更
4. 推荐缓存设置合理过期时间，避免陈旧结果长期存在