param(
    [string]$Root = "."
)

$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path $Root).Path
$docsRoot = Join-Path $projectRoot "docs"
$files = Get-ChildItem -Path $projectRoot -Recurse -File -Include *.md
$missing = New-Object System.Collections.Generic.List[string]

function Resolve-MarkdownReference {
    param(
        [string]$CurrentFile,
        [string]$Reference,
        [string]$ProjectRoot,
        [string]$DocsRoot
    )

    $candidates = New-Object System.Collections.Generic.List[string]
    $currentDir = Split-Path $CurrentFile -Parent

    if ([System.IO.Path]::IsPathRooted($Reference)) {
        $candidates.Add($Reference)
    } else {
        $candidates.Add((Join-Path $currentDir $Reference))
        $candidates.Add((Join-Path $ProjectRoot $Reference))
        $candidates.Add((Join-Path $DocsRoot $Reference))
        if ($Reference.StartsWith("docs/")) {
            $trimmed = $Reference.Substring(5)
            $candidates.Add((Join-Path $DocsRoot $trimmed))
        }
    }

    foreach ($candidate in $candidates | Select-Object -Unique) {
        try {
            $resolved = [System.IO.Path]::GetFullPath($candidate)
            if (Test-Path $resolved) {
                return $resolved
            }
        } catch {
        }
    }

    return $null
}

foreach ($file in $files) {
    $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8
    $matches = [regex]::Matches($content, '([A-Za-z0-9_./\-*]+\.md)')
    foreach ($match in $matches) {
        $reference = $match.Groups[1].Value
        if ($reference -match '^https?://') {
            continue
        }
        if ($reference -eq '.md' -or $reference.Contains('*')) {
            continue
        }
        $resolved = Resolve-MarkdownReference -CurrentFile $file.FullName -Reference $reference -ProjectRoot $projectRoot -DocsRoot $docsRoot
        if (-not $resolved) {
            $missing.Add("$($file.FullName) -> $reference")
        }
    }
}

if ($missing.Count -gt 0) {
    Write-Host "Missing markdown references:" -ForegroundColor Red
    $missing | Sort-Object -Unique | ForEach-Object { Write-Host " - $_" }
    exit 1
}

Write-Host "Markdown reference check passed." -ForegroundColor Green
