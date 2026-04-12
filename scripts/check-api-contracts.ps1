param(
    [string]$Root = "."
)

$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path $Root).Path
$dtoRoot = Join-Path $projectRoot "src\main\java\com\lcj\campusreco\domain\dto"
$voRoot = Join-Path $projectRoot "src\main\java\com\lcj\campusreco\domain\vo"
$controllerRoot = Join-Path $projectRoot "src\main\java\com\lcj\campusreco\controller"

$expectedFields = @{
    (Join-Path $dtoRoot "UserCreateDTO.java") = @("nickname", "grade", "major", "college", "bio")
    (Join-Path $dtoRoot "UserTagBindDTO.java") = @("tagIds", "sourceType")
    (Join-Path $dtoRoot "FeedbackSubmitDTO.java") = @("recommendationId", "targetUserId", "feedbackType")
    (Join-Path $dtoRoot "RecommendRequestDTO.java") = @("userId", "topK", "useCache")
    (Join-Path $voRoot "UserProfileVO.java") = @("profileVersion", "topkTags", "updatedAt", "profileJson", "topkJson")
    (Join-Path $voRoot "RecommendationItemVO.java") = @("targetUserId", "finalScore", "rankNo", "explanation")
    (Join-Path $voRoot "RecommendationDetailVO.java") = @("requestTraceId", "items", "recallCandidatesCount", "rankingDetails", "rerankRuleHits", "explanationEvidence")
    (Join-Path $voRoot "ExplanationVO.java") = @("reasonText", "evidenceJson", "contributionJson")
}

$expectedControllerMarkers = @{
    (Join-Path $controllerRoot "UserController.java") = @('"userId"')
    (Join-Path $controllerRoot "RecommendationController.java") = @('@RequestParam(defaultValue = "10") Integer topK', '@RequestParam(defaultValue = "true") Boolean useCache')
    (Join-Path $controllerRoot "FeedbackController.java") = @('"profileUpdated"')
}

$missing = New-Object System.Collections.Generic.List[string]

foreach ($entry in $expectedFields.GetEnumerator()) {
    $filePath = $entry.Key
    if (-not (Test-Path $filePath)) {
        $missing.Add("Missing contract file: $filePath")
        continue
    }
    $content = Get-Content -Path $filePath -Raw -Encoding UTF8
    foreach ($field in $entry.Value) {
        if (-not $content.Contains($field)) {
            $missing.Add("$filePath -> missing field marker: $field")
        }
    }
}

foreach ($entry in $expectedControllerMarkers.GetEnumerator()) {
    $filePath = $entry.Key
    if (-not (Test-Path $filePath)) {
        $missing.Add("Missing controller file: $filePath")
        continue
    }
    $content = Get-Content -Path $filePath -Raw -Encoding UTF8
    foreach ($marker in $entry.Value) {
        if (-not $content.Contains($marker)) {
            $missing.Add("$filePath -> missing controller marker: $marker")
        }
    }
}

if ($missing.Count -gt 0) {
    Write-Host "API contract check failed." -ForegroundColor Red
    $missing | Sort-Object | ForEach-Object { Write-Host " - $_" }
    exit 1
}

Write-Host "API contract check passed." -ForegroundColor Green
