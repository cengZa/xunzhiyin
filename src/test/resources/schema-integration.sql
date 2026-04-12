DROP TABLE IF EXISTS recommendation_explanation;
DROP TABLE IF EXISTS recommendation_result;
DROP TABLE IF EXISTS user_feedback;
DROP TABLE IF EXISTS user_profile;
DROP TABLE IF EXISTS user_tag_relation;
DROP TABLE IF EXISTS tag;
DROP TABLE IF EXISTS user;

CREATE TABLE user (
  id BIGINT PRIMARY KEY,
  student_no VARCHAR(32) NOT NULL DEFAULT '',
  nickname VARCHAR(64) NOT NULL DEFAULT '',
  gender TINYINT NOT NULL DEFAULT 0,
  grade INT NOT NULL DEFAULT 0,
  major VARCHAR(64) NOT NULL DEFAULT '',
  college VARCHAR(64) NOT NULL DEFAULT '',
  bio VARCHAR(255) NOT NULL DEFAULT '',
  status TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_student_no ON user(student_no);
CREATE INDEX idx_major_grade ON user(major, grade);

CREATE TABLE tag (
  id BIGINT PRIMARY KEY,
  tag_name VARCHAR(64) NOT NULL DEFAULT '',
  tag_type VARCHAR(32) NOT NULL DEFAULT '',
  tag_desc VARCHAR(255) NOT NULL DEFAULT '',
  status TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_tag_name_type ON tag(tag_name, tag_type);

CREATE TABLE user_tag_relation (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  source_type VARCHAR(32) NOT NULL DEFAULT 'manual',
  selected_at TIMESTAMP NULL,
  weight_seed DECIMAL(10, 4) NOT NULL DEFAULT 1.0000,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_user_tag ON user_tag_relation(user_id, tag_id);
CREATE INDEX idx_user_tag_relation_tag_id ON user_tag_relation(tag_id);
CREATE INDEX idx_user_tag_relation_user_id ON user_tag_relation(user_id);

CREATE TABLE user_profile (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  profile_version INT NOT NULL DEFAULT 1,
  profile_json CLOB NOT NULL,
  topk_json CLOB NOT NULL,
  updated_by VARCHAR(32) NOT NULL DEFAULT 'init',
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_user_profile_version ON user_profile(user_id, profile_version);
CREATE INDEX idx_user_profile_user_id ON user_profile(user_id);

CREATE TABLE recommendation_result (
  id BIGINT PRIMARY KEY,
  request_user_id BIGINT NOT NULL,
  target_user_id BIGINT NOT NULL,
  recall_score DECIMAL(10, 4) NOT NULL DEFAULT 0.0000,
  rank_score DECIMAL(10, 4) NOT NULL DEFAULT 0.0000,
  rerank_score DECIMAL(10, 4) NOT NULL DEFAULT 0.0000,
  final_score DECIMAL(10, 4) NOT NULL DEFAULT 0.0000,
  rank_no INT NOT NULL DEFAULT 0,
  request_trace_id VARCHAR(64) NOT NULL DEFAULT '',
  created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_recommendation_request_trace ON recommendation_result(request_user_id, request_trace_id);
CREATE INDEX idx_recommendation_target_user ON recommendation_result(target_user_id);

CREATE TABLE recommendation_explanation (
  id BIGINT PRIMARY KEY,
  recommendation_id BIGINT NOT NULL,
  reason_text VARCHAR(500) NOT NULL DEFAULT '',
  evidence_json CLOB NOT NULL,
  contribution_json CLOB NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_recommendation_explanation_recommendation_id ON recommendation_explanation(recommendation_id);

CREATE TABLE user_feedback (
  id BIGINT PRIMARY KEY,
  request_user_id BIGINT NOT NULL,
  target_user_id BIGINT NOT NULL,
  recommendation_id BIGINT NOT NULL,
  feedback_type VARCHAR(32) NOT NULL DEFAULT '',
  feedback_time TIMESTAMP NOT NULL,
  feedback_note VARCHAR(255) NOT NULL DEFAULT '',
  created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_user_feedback_request_user ON user_feedback(request_user_id);
CREATE INDEX idx_user_feedback_recommendation_id ON user_feedback(recommendation_id);
