param(
    [string]$Root = "."
)

$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path $Root).Path
$checks = @(
    "check-doc-links.ps1",
    "check-backend-skeleton.ps1",
    "check-module-boundaries.ps1",
    "check-service-signatures.ps1",
    "check-controller-routes.ps1",
    "check-api-contracts.ps1",
    "check-java-toolchain.ps1"
)

foreach ($check in $checks) {
    $path = Join-Path $projectRoot "scripts\$check"
    Write-Host "Running $check ..." -ForegroundColor Cyan
    if ($check -eq "check-java-toolchain.ps1") {
        & powershell -ExecutionPolicy Bypass -File $path -RequiredMajor 21
    } else {
        & powershell -ExecutionPolicy Bypass -File $path -Root $projectRoot
    }
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

Write-Host "All governance checks passed." -ForegroundColor Green
