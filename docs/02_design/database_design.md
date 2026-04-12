# Database Design
# 数据库设计

## 1. 设计原则

1. 以支撑推荐主链路为核心
2. 结构尽量简洁，避免为论文堆表
3. 能支撑推荐、解释、反馈、测试与论文展示
4. 表结构与接口字段命名保持一致

## 2. 核心表清单

### 2.1 user
用途：存储用户基础信息

字段建议：
- id bigint PK
- student_no varchar(32)
- nickname varchar(64)
- gender tinyint
- grade int
- major varchar(64)
- college varchar(64)
- bio varchar(255)
- status tinyint
- created_at datetime
- updated_at datetime

### 2.2 tag
用途：存储标签定义

字段建议：
- id bigint PK
- tag_name varchar(64)
- tag_type varchar(32)  # academic / hobby / club / grade_major
- tag_desc varchar(255)
- status tinyint
- created_at datetime
- updated_at datetime

### 2.3 user_tag_relation
用途：存储用户与标签关系，是画像计算原始数据来源

字段建议：
- id bigint PK
- user_id bigint
- tag_id bigint
- source_type varchar(32)   # manual / inferred / feedback
- selected_at datetime
- weight_seed decimal(10,4) # 原始权重种子，可选
- created_at datetime
- updated_at datetime

### 2.4 user_profile
用途：存储用户画像聚合结果

字段建议：
- id bigint PK
- user_id bigint
- profile_version int
- profile_json text          # 标签向量 JSON
- topk_json text             # Top-K 标签 JSON
- updated_by varchar(32)     # init / feedback / rebuild
- created_at datetime
- updated_at datetime

### 2.5 recommendation_result
用途：存储一次推荐任务的结果

字段建议：
- id bigint PK
- request_user_id bigint
- target_user_id bigint
- recall_score decimal(10,4)
- rank_score decimal(10,4)
- rerank_score decimal(10,4)
- final_score decimal(10,4)
- rank_no int
- request_trace_id varchar(64)
- created_at datetime

### 2.6 recommendation_explanation
用途：存储推荐解释与证据项

字段建议：
- id bigint PK
- recommendation_id bigint
- reason_text varchar(500)
- evidence_json text
- contribution_json text
- created_at datetime

### 2.7 user_feedback
用途：记录用户对推荐结果的反馈

字段建议：
- id bigint PK
- request_user_id bigint
- target_user_id bigint
- recommendation_id bigint
- feedback_type varchar(32)  # follow / ignore
- feedback_time datetime
- feedback_note varchar(255)
- created_at datetime

## 3. 表关系

1. user 与 user_tag_relation 是一对多
2. tag 与 user_tag_relation 是一对多
3. user 与 user_profile 是一对一或一对多版本
4. recommendation_result 由 request_user_id 与 target_user_id 构成一次推荐明细
5. recommendation_explanation 与 recommendation_result 是一对一或一对多
6. user_feedback 与 recommendation_result 是多对一

## 4. 设计说明

### 4.1 为什么单独保留 user_profile
因为画像是聚合后的中间产物，后续召回、排序、反馈更新都会反复读取，单独存表可以减少每次现算。

### 4.2 为什么 recommendation_result 与 recommendation_explanation 分表
因为解释数据往往包含结构化证据 JSON，单独存储更清晰，也便于论文展示“排序结果”和“解释结果”的对应关系。

### 4.3 为什么保留 request_trace_id
便于一次推荐任务中串联多个 target_user_id 结果，支持调试、测试与答辩演示。

## 5. 为测试和论文预留的字段建议

建议额外记录：
- recall_candidate_count
- rerank_rule_hit_json
- explanation_consistency_flag
- response_time_ms

这些字段可不直接存入核心表，也可以通过日志表或埋点表记录，用于后续验证：
- Precision@10
- 候选召回覆盖率
- 场景约束满足率
- 解释一致率