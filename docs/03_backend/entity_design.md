# Entity Design
# 实体与模型设计

## 1. 数据库实体（Entity）

### 1.1 UserEntity
对应表：user

核心字段：
- Long id
- String studentNo
- String nickname
- Integer gender
- Integer grade
- String major
- String college
- String bio
- Integer status
- LocalDateTime createdAt
- LocalDateTime updatedAt

### 1.2 TagEntity
对应表：tag

核心字段：
- Long id
- String tagName
- String tagType
- String tagDesc
- Integer status
- LocalDateTime createdAt
- LocalDateTime updatedAt

### 1.3 UserTagRelationEntity
对应表：user_tag_relation

核心字段：
- Long id
- Long userId
- Long tagId
- String sourceType
- LocalDateTime selectedAt
- BigDecimal weightSeed
- LocalDateTime createdAt
- LocalDateTime updatedAt

### 1.4 UserProfileEntity
对应表：user_profile

核心字段：
- Long id
- Long userId
- Integer profileVersion
- String profileJson
- String topkJson
- String updatedBy
- LocalDateTime createdAt
- LocalDateTime updatedAt

### 1.5 RecommendationResultEntity
对应表：recommendation_result

核心字段：
- Long id
- Long requestUserId
- Long targetUserId
- BigDecimal recallScore
- BigDecimal rankScore
- BigDecimal rerankScore
- BigDecimal finalScore
- Integer rankNo
- String requestTraceId
- LocalDateTime createdAt

### 1.6 RecommendationExplanationEntity
对应表：recommendation_explanation

核心字段：
- Long id
- Long recommendationId
- String reasonText
- String evidenceJson
- String contributionJson
- LocalDateTime createdAt

### 1.7 UserFeedbackEntity
对应表：user_feedback

核心字段：
- Long id
- Long requestUserId
- Long targetUserId
- Long recommendationId
- String feedbackType
- LocalDateTime feedbackTime
- String feedbackNote
- LocalDateTime createdAt

## 2. 推荐计算模型（Model）

### 2.1 TagWeightModel
用途：表示画像中的单个标签权重项

字段：
- Long tagId
- String tagName
- String tagType
- BigDecimal tf
- BigDecimal idf
- BigDecimal timeDecay
- BigDecimal finalWeight

### 2.2 UserProfileModel
用途：内存中的用户画像对象

字段：
- Long userId
- List<TagWeightModel> tagWeights
- Map<Long, BigDecimal> vector
- List<TagWeightModel> topKTags
- Integer profileVersion

### 2.3 RankingCandidateModel
用途：召回+排序阶段的候选对象

字段：
- Long targetUserId
- BigDecimal recallScore
- BigDecimal rankScore
- BigDecimal rerankScore
- BigDecimal finalScore
- List<ContributionItemModel> contributions
- List<RuleHitModel> ruleHits

### 2.4 ContributionItemModel
用途：记录标签贡献项

字段：
- Long tagId
- String tagName
- BigDecimal sourceWeight
- BigDecimal targetWeight
- BigDecimal contributionScore

### 2.5 RuleHitModel
用途：记录重排命中规则

字段：
- String ruleCode
- String ruleDesc
- Boolean hit
- BigDecimal adjustScore

## 3. 请求对象（DTO）

### 3.1 RecommendRequestDTO
字段：
- Long userId
- Integer topK
- Boolean useCache

### 3.2 FeedbackSubmitDTO
字段：
- Long recommendationId
- Long targetUserId
- String feedbackType

### 3.3 BuildProfileDTO
字段：
- Long userId
- String updatedBy

## 4. 返回对象（VO）

### 4.1 RecommendationItemVO
字段：
- Long recommendationId
- Long targetUserId
- String targetNickname
- BigDecimal finalScore
- Integer rankNo
- String reasonText

### 4.2 RecommendationDetailVO
字段：
- String requestTraceId
- Integer recallCandidateCount
- List<RecommendationItemVO> items

### 4.3 ExplanationVO
字段：
- Long recommendationId
- String reasonText
- Object evidence
- Object contribution