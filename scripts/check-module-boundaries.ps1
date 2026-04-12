param(
    [string]$Root = "."
)

$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path $Root).Path
$packageRoot = Join-Path $projectRoot "src\main\java\com\lcj\campusreco"

$allowedDirectories = @(
    "common",
    "config",
    "controller",
    "domain",
    "infra",
    "mapper",
    "service",
    "strategy"
)

$allowedFiles = @(
    "CampusRecoApplication.java"
)

$violations = New-Object System.Collections.Generic.List[string]

Get-ChildItem -Path $packageRoot -Directory | ForEach-Object {
    if ($allowedDirectories -notcontains $_.Name) {
        $violations.Add("Unexpected top-level package: $($_.FullName)")
    }
}

Get-ChildItem -Path $packageRoot -File | ForEach-Object {
    if ($allowedFiles -notcontains $_.Name) {
        $violations.Add("Unexpected top-level file: $($_.FullName)")
    }
}

if ($violations.Count -gt 0) {
    Write-Host "Module boundary check failed." -ForegroundColor Red
    $violations | Sort-Object | ForEach-Object { Write-Host " - $_" }
    exit 1
}

Write-Host "Module boundary check passed." -ForegroundColor Green
