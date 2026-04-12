CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL,
  `student_no` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '学号',
  `nickname` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '昵称',
  `gender` TINYINT NOT NULL DEFAULT 0 COMMENT '性别: 0未知 1男 2女',
  `grade` INT NOT NULL DEFAULT 0 COMMENT '年级',
  `major` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '专业',
  `college` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '学院',
  `bio` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '个人简介',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1有效 0无效',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_no` (`student_no`),
  KEY `idx_major_grade` (`major`, `grade`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基础信息表';

CREATE TABLE IF NOT EXISTS `tag` (
  `id` BIGINT NOT NULL,
  `tag_name` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '标签名称',
  `tag_type` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '标签类型: academic/hobby/club/grade_major',
  `tag_desc` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '标签描述',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1有效 0无效',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_name_type` (`tag_name`, `tag_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签定义表';

CREATE TABLE IF NOT EXISTS `user_tag_relation` (
  `id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `tag_id` BIGINT NOT NULL COMMENT '标签ID',
  `source_type` VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT '来源类型: manual/inferred/feedback',
  `selected_at` DATETIME DEFAULT NULL COMMENT '选择或最近活跃时间',
  `weight_seed` DECIMAL(10,4) NOT NULL DEFAULT 1.0000 COMMENT '初始权重种子',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tag` (`user_id`, `tag_id`),
  KEY `idx_tag_id` (`tag_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户标签关系表';

CREATE TABLE IF NOT EXISTS `user_profile` (
  `id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `profile_version` INT NOT NULL DEFAULT 1 COMMENT '画像版本号',
  `profile_json` TEXT NOT NULL COMMENT '完整画像JSON',
  `topk_json` TEXT NOT NULL COMMENT 'Top-K标签JSON',
  `updated_by` VARCHAR(32) NOT NULL DEFAULT 'init' COMMENT '更新来源: init/feedback/rebuild',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_profile_version` (`user_id`, `profile_version`),
  KEY `idx_user_profile_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户画像表';

CREATE TABLE IF NOT EXISTS `recommendation_result` (
  `id` BIGINT NOT NULL,
  `request_user_id` BIGINT NOT NULL COMMENT '发起推荐请求的用户ID',
  `target_user_id` BIGINT NOT NULL COMMENT '被推荐用户ID',
  `recall_score` DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '召回粗分',
  `rank_score` DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '排序得分',
  `rerank_score` DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '重排增量得分',
  `final_score` DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '最终得分',
  `rank_no` INT NOT NULL DEFAULT 0 COMMENT '排序名次',
  `request_trace_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '单次推荐链路ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_request_user_trace` (`request_user_id`, `request_trace_id`),
  KEY `idx_target_user_id` (`target_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐结果表';

CREATE TABLE IF NOT EXISTS `recommendation_explanation` (
  `id` BIGINT NOT NULL,
  `recommendation_id` BIGINT NOT NULL COMMENT '推荐结果ID',
  `reason_text` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '解释文本',
  `evidence_json` TEXT NOT NULL COMMENT '解释证据JSON',
  `contribution_json` TEXT NOT NULL COMMENT '贡献项JSON',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_recommendation_id` (`recommendation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐解释表';

CREATE TABLE IF NOT EXISTS `user_feedback` (
  `id` BIGINT NOT NULL,
  `request_user_id` BIGINT NOT NULL COMMENT '反馈发起用户ID',
  `target_user_id` BIGINT NOT NULL COMMENT '被反馈用户ID',
  `recommendation_id` BIGINT NOT NULL COMMENT '关联推荐结果ID',
  `feedback_type` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '反馈类型: follow/ignore',
  `feedback_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '反馈时间',
  `feedback_note` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_request_user_id` (`request_user_id`),
  KEY `idx_recommendation_id` (`recommendation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';
