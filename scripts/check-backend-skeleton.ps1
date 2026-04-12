param(
    [string]$Root = "."
)

$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path $Root).Path
$srcRoot = Join-Path $projectRoot "src\main\java\com\lcj\campusreco"
$resourceRoot = Join-Path $projectRoot "src\main\resources"
$testRoot = Join-Path $projectRoot "src\test\java\com\lcj\campusreco"

$expected = @(
    "CampusRecoApplication.java",
    "common\api\ApiResponse.java",
    "common\api\PageResponse.java",
    "common\constant\RedisKeys.java",
    "common\constant\FeedbackType.java",
    "common\constant\TagType.java",
    "common\exception\BizException.java",
    "common\exception\GlobalExceptionHandler.java",
    "common\util\JsonUtils.java",
    "common\util\VectorUtils.java",
    "common\util\TimeDecayUtils.java",
    "config\RedisConfig.java",
    "config\JacksonConfig.java",
    "config\MybatisPlusConfig.java",
    "controller\UserController.java",
    "controller\TagController.java",
    "controller\ProfileController.java",
    "controller\RecommendationController.java",
    "controller\FeedbackController.java",
    "domain\entity\UserEntity.java",
    "domain\entity\TagEntity.java",
    "domain\entity\UserTagRelationEntity.java",
    "domain\entity\UserProfileEntity.java",
    "domain\entity\RecommendationResultEntity.java",
    "domain\entity\RecommendationExplanationEntity.java",
    "domain\entity\UserFeedbackEntity.java",
    "domain\dto\BuildProfileDTO.java",
    "domain\dto\RecommendRequestDTO.java",
    "domain\dto\FeedbackSubmitDTO.java",
    "domain\dto\ExplanationGenerateDTO.java",
    "domain\vo\UserVO.java",
    "domain\vo\UserProfileVO.java",
    "domain\vo\RecommendationItemVO.java",
    "domain\vo\RecommendationDetailVO.java",
    "domain\vo\ExplanationVO.java",
    "domain\model\TagWeightModel.java",
    "domain\model\UserProfileModel.java",
    "domain\model\RankingCandidateModel.java",
    "domain\model\ContributionItemModel.java",
    "domain\model\RuleHitModel.java",
    "mapper\UserMapper.java",
    "mapper\TagMapper.java",
    "mapper\UserTagRelationMapper.java",
    "mapper\UserProfileMapper.java",
    "mapper\RecommendationResultMapper.java",
    "mapper\RecommendationExplanationMapper.java",
    "mapper\UserFeedbackMapper.java",
    "service\UserService.java",
    "service\TagService.java",
    "service\ProfileService.java",
    "service\RecallService.java",
    "service\RankingService.java",
    "service\RerankService.java",
    "service\ExplanationService.java",
    "service\FeedbackService.java",
    "service\RecommendationService.java",
    "service\impl\UserServiceImpl.java",
    "service\impl\TagServiceImpl.java",
    "service\impl\ProfileServiceImpl.java",
    "service\impl\RecallServiceImpl.java",
    "service\impl\RankingServiceImpl.java",
    "service\impl\RerankServiceImpl.java",
    "service\impl\ExplanationServiceImpl.java",
    "service\impl\FeedbackServiceImpl.java",
    "service\impl\RecommendationServiceImpl.java",
    "strategy\profile\ProfileWeightCalculator.java",
    "strategy\profile\ImprovedTfIdfProfileWeightCalculator.java",
    "strategy\rerank\RerankRule.java",
    "strategy\rerank\GradeDiffRule.java",
    "strategy\rerank\MajorRelatedRule.java",
    "strategy\rerank\ClubOverlapRule.java",
    "strategy\explain\ExplanationTemplateBuilder.java",
    "strategy\explain\ExplanationEvidenceExtractor.java",
    "infra\redis\RecallIndexRepository.java",
    "infra\redis\ProfileCacheRepository.java",
    "infra\repository\RecommendationQueryRepository.java"
)

$expectedResourceFiles = @(
    (Join-Path $resourceRoot "application.yml"),
    (Join-Path $testRoot "CampusRecoApplicationTests.java")
)

$missing = New-Object System.Collections.Generic.List[string]

foreach ($item in $expected) {
    $fullPath = Join-Path $srcRoot $item
    if (-not (Test-Path $fullPath)) {
        $missing.Add($fullPath)
    }
}

foreach ($item in $expectedResourceFiles) {
    if (-not (Test-Path $item)) {
        $missing.Add($item)
    }
}

if ($missing.Count -gt 0) {
    Write-Host "Missing backend skeleton files:" -ForegroundColor Red
    $missing | Sort-Object | ForEach-Object { Write-Host " - $_" }
    exit 1
}

Write-Host "Backend skeleton check passed." -ForegroundColor Green
