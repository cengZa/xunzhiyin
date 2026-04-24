param(
    [Parameter(Mandatory = $true)]
    [string]$InputPath,

    [string]$OutputPath,

    [switch]$NumberLines
)

$ErrorActionPreference = "Stop"

$resolvedInput = (Resolve-Path $InputPath).Path
$tempCopy = Join-Path $env:TEMP ("docx_extract_" + [System.Guid]::NewGuid().ToString() + ".docx")

Copy-Item -LiteralPath $resolvedInput -Destination $tempCopy -Force

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($tempCopy)

try {
    $entry = $zip.Entries | Where-Object { $_.FullName -eq "word/document.xml" }
    if (-not $entry) {
        throw "word/document.xml not found in $resolvedInput"
    }

    $reader = New-Object System.IO.StreamReader($entry.Open())
    $xml = $reader.ReadToEnd()
    $reader.Close()

    $text = [regex]::Replace($xml, "<w:tab[^>]*/>", "`t")
    $text = [regex]::Replace($text, "</w:p>", "`r`n")
    $text = [regex]::Replace($text, "<[^>]+>", "")
    $text = [System.Net.WebUtility]::HtmlDecode($text)

    $lines = $text -split "`r?`n" |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ -ne "" }

    if ($NumberLines) {
        $index = 1
        $lines = $lines | ForEach-Object {
            $formatted = "{0,4}: {1}" -f $index, $_
            $index++
            $formatted
        }
    }

    if ($OutputPath) {
        $resolvedOutput = $OutputPath
        if (-not [System.IO.Path]::IsPathRooted($resolvedOutput)) {
            $resolvedOutput = Join-Path (Get-Location) $resolvedOutput
        }

        $parent = Split-Path -Parent $resolvedOutput
        if ($parent -and -not (Test-Path -LiteralPath $parent)) {
            New-Item -ItemType Directory -Path $parent | Out-Null
        }

        [System.IO.File]::WriteAllLines($resolvedOutput, $lines, [System.Text.UTF8Encoding]::new($true))
        Write-Output "Wrote: $resolvedOutput"
    } else {
        $lines
    }
}
finally {
    $zip.Dispose()
    Remove-Item -LiteralPath $tempCopy -Force -ErrorAction SilentlyContinue
}
