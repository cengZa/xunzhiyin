param(
    [int]$RequiredMajor = 21
)

$ErrorActionPreference = "Stop"

$startInfo = New-Object System.Diagnostics.ProcessStartInfo
$startInfo.FileName = "java"
$startInfo.Arguments = "-version"
$startInfo.RedirectStandardError = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.UseShellExecute = $false
$startInfo.CreateNoWindow = $true

$process = New-Object System.Diagnostics.Process
$process.StartInfo = $startInfo
$null = $process.Start()
$stdout = $process.StandardOutput.ReadToEnd()
$stderr = $process.StandardError.ReadToEnd()
$process.WaitForExit()

if ($process.ExitCode -ne 0 -and [string]::IsNullOrWhiteSpace($stderr) -and [string]::IsNullOrWhiteSpace($stdout)) {
    Write-Host "Java toolchain check failed: java command unavailable." -ForegroundColor Red
    exit 1
}

$combined = ($stdout + "`n" + $stderr).Trim()
$firstLine = ($combined -split "`r?`n" | Select-Object -First 1)
if ($firstLine -notmatch '"(?<version>[0-9]+)') {
    Write-Host "Java toolchain check failed: unable to parse java version." -ForegroundColor Red
    Write-Host $firstLine
    exit 1
}

$major = [int]$Matches['version']
if ($major -lt $RequiredMajor) {
    Write-Host "Java toolchain check failed: current JDK is $major, required is $RequiredMajor." -ForegroundColor Red
    exit 1
}

Write-Host "Java toolchain check passed." -ForegroundColor Green
