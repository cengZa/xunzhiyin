param(
    [string]$Root = "."
)

$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path $Root).Path
$serviceRoot = Join-Path $projectRoot "src\main\java\com\lcj\campusreco\service"

$expected = @{
    "UserService.java" = @(
        "Long createUser(UserCreateDTO dto);",
        "UserEntity getById(Long userId);",
        "List<UserEntity> listByIds(List<Long> userIds);"
    )
    "TagService.java" = @(
        "void bindUserTags(Long userId, List<Long> tagIds, String sourceType);",
        "List<TagEntity> listUserTags(Long userId);"
    )
    "ProfileService.java" = @(
        "UserProfileModel buildProfile(Long userId, String updatedBy);",
        "UserProfileModel getProfile(Long userId);",
        "void rebuildProfile(Long userId, String updatedBy);"
    )
    "RecallService.java" = @(
        "Set<Long> recallCandidateUserIds(UserProfileModel profile);"
    )
    "RankingService.java" = @(
        "List<RankingCandidateModel> rank(Long requestUserId, Set<Long> candidateUserIds);"
    )
    "RerankService.java" = @(
        "List<RankingCandidateModel> rerank(Long requestUserId, List<RankingCandidateModel> rankingList);"
    )
    "ExplanationService.java" = @(
        "ExplanationVO generate(RankingCandidateModel candidate);",
        "void batchSaveExplanation(List<RankingCandidateModel> candidates, Map<Long, Long> recommendationIdMap);"
    )
    "FeedbackService.java" = @(
        "void submitFeedback(Long requestUserId, FeedbackSubmitDTO dto);",
        "void applyFeedbackUpdate(Long requestUserId, Long recommendationId, String feedbackType);"
    )
    "RecommendationService.java" = @(
        "RecommendationDetailVO recommend(RecommendRequestDTO dto);"
    )
}

$missing = New-Object System.Collections.Generic.List[string]

foreach ($entry in $expected.GetEnumerator()) {
    $filePath = Join-Path $serviceRoot $entry.Key
    if (-not (Test-Path $filePath)) {
        $missing.Add("Missing service interface file: $filePath")
        continue
    }

    $content = Get-Content -Path $filePath -Raw -Encoding UTF8
    foreach ($signature in $entry.Value) {
        if (-not $content.Contains($signature)) {
            $missing.Add("$($entry.Key) -> missing signature: $signature")
        }
    }
}

if ($missing.Count -gt 0) {
    Write-Host "Service signature check failed." -ForegroundColor Red
    $missing | Sort-Object | ForEach-Object { Write-Host " - $_" }
    exit 1
}

Write-Host "Service signature check passed." -ForegroundColor Green
