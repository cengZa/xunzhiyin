param(
    [string]$Root = "."
)

$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path $Root).Path
$controllerRoot = Join-Path $projectRoot "src\main\java\com\lcj\campusreco\controller"

$expected = @{
    "UserController.java" = @(
        '@RequestMapping("/api/users")',
        '@PostMapping',
        '@GetMapping("/{userId}")',
        '@PostMapping("/{userId}/tags")',
        '@GetMapping("/{userId}/tags")'
    )
    "ProfileController.java" = @(
        '@RequestMapping("/api/profiles")',
        '@PostMapping("/{userId}/build")',
        '@GetMapping("/{userId}")'
    )
    "RecommendationController.java" = @(
        '@RequestMapping("/api/recommendations")',
        '@GetMapping("/{userId}")',
        '@GetMapping("/{userId}/detail")',
        '@GetMapping("/{recommendationId}/explanation")'
    )
    "FeedbackController.java" = @(
        '@PostMapping("/api/recommendations/{userId}/feedback")',
        '@GetMapping("/api/feedback/{userId}")'
    )
}

$missing = New-Object System.Collections.Generic.List[string]

foreach ($entry in $expected.GetEnumerator()) {
    $filePath = Join-Path $controllerRoot $entry.Key
    if (-not (Test-Path $filePath)) {
        $missing.Add("Missing controller file: $filePath")
        continue
    }

    $content = Get-Content -Path $filePath -Raw -Encoding UTF8
    foreach ($route in $entry.Value) {
        if (-not $content.Contains($route)) {
            $missing.Add("$($entry.Key) -> missing route marker: $route")
        }
    }
}

if ($missing.Count -gt 0) {
    Write-Host "Controller route check failed." -ForegroundColor Red
    $missing | Sort-Object | ForEach-Object { Write-Host " - $_" }
    exit 1
}

Write-Host "Controller route check passed." -ForegroundColor Green
