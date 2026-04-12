# Backend Structure
# 后端工程结构设计

## 1. 工程目标

本工程采用 Spring Boot 单体应用架构，围绕推荐主链路进行模块化拆分，保证：
1. 业务主链路清晰
2. 模块职责边界稳定
3. 便于单独测试与逐步实现
4. 适合本科毕设规模，不做过度设计

## 2. 推荐包结构

建议包名：
com.lcj.campusreco

目录结构建议如下：

src/main/java/com/lcj/campusreco/
├── CampusRecoApplication.java
├── common/
│   ├── api/
│   │   ├── ApiResponse.java
│   │   └── PageResponse.java
│   ├── constant/
│   │   ├── RedisKeys.java
│   │   ├── FeedbackType.java
│   │   └── TagType.java
│   ├── exception/
│   │   ├── BizException.java
│   │   └── GlobalExceptionHandler.java
│   └── util/
│       ├── JsonUtils.java
│       ├── VectorUtils.java
│       └── TimeDecayUtils.java
│
├── config/
│   ├── RedisConfig.java
│   ├── JacksonConfig.java
│   └── MybatisPlusConfig.java
│
├── controller/
│   ├── UserController.java
│   ├── TagController.java
│   ├── ProfileController.java
│   ├── RecommendationController.java
│   └── FeedbackController.java
│
├── domain/
│   ├── entity/
│   │   ├── UserEntity.java
│   │   ├── TagEntity.java
│   │   ├── UserTagRelationEntity.java
│   │   ├── UserProfileEntity.java
│   │   ├── RecommendationResultEntity.java
│   │   ├── RecommendationExplanationEntity.java
│   │   └── UserFeedbackEntity.java
│   ├── dto/
│   │   ├── BuildProfileDTO.java
│   │   ├── RecommendRequestDTO.java
│   │   ├── FeedbackSubmitDTO.java
│   │   └── ExplanationGenerateDTO.java
│   ├── vo/
│   │   ├── UserVO.java
│   │   ├── UserProfileVO.java
│   │   ├── RecommendationItemVO.java
│   │   ├── RecommendationDetailVO.java
│   │   └── ExplanationVO.java
│   └── model/
│       ├── TagWeightModel.java
│       ├── UserProfileModel.java
│       ├── RankingCandidateModel.java
│       ├── ContributionItemModel.java
│       └── RuleHitModel.java
│
├── mapper/
│   ├── UserMapper.java
│   ├── TagMapper.java
│   ├── UserTagRelationMapper.java
│   ├── UserProfileMapper.java
│   ├── RecommendationResultMapper.java
│   ├── RecommendationExplanationMapper.java
│   └── UserFeedbackMapper.java
│
├── service/
│   ├── UserService.java
│   ├── TagService.java
│   ├── ProfileService.java
│   ├── RecallService.java
│   ├── RankingService.java
│   ├── RerankService.java
│   ├── ExplanationService.java
│   ├── FeedbackService.java
│   └── RecommendationService.java
│
├── service/impl/
│   ├── UserServiceImpl.java
│   ├── TagServiceImpl.java
│   ├── ProfileServiceImpl.java
│   ├── RecallServiceImpl.java
│   ├── RankingServiceImpl.java
│   ├── RerankServiceImpl.java
│   ├── ExplanationServiceImpl.java
│   ├── FeedbackServiceImpl.java
│   └── RecommendationServiceImpl.java
│
├── strategy/
│   ├── profile/
│   │   ├── ProfileWeightCalculator.java
│   │   └── ImprovedTfIdfProfileWeightCalculator.java
│   ├── rerank/
│   │   ├── RerankRule.java
│   │   ├── GradeDiffRule.java
│   │   ├── MajorRelatedRule.java
│   │   └── ClubOverlapRule.java
│   └── explain/
│       ├── ExplanationTemplateBuilder.java
│       └── ExplanationEvidenceExtractor.java
│
└── infra/
    ├── redis/
    │   ├── RecallIndexRepository.java
    │   └── ProfileCacheRepository.java
    └── repository/
        └── RecommendationQueryRepository.java