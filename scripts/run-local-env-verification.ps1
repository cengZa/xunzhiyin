$ErrorActionPreference = "Stop"

if (-not $env:JAVA_HOME) {
    throw "JAVA_HOME is not set. Please point it to JDK 21 before running this script."
}

$mysqlHost = "127.0.0.1"
$mysqlPort = 3306
$redisHost = "127.0.0.1"
$redisPort = 6379
$databaseName = "campus_reco"

$mysqlReady = Test-NetConnection -ComputerName $mysqlHost -Port $mysqlPort -WarningAction SilentlyContinue
$redisReady = Test-NetConnection -ComputerName $redisHost -Port $redisPort -WarningAction SilentlyContinue

if (-not $redisReady.TcpTestSucceeded) {
    $redisService = Get-Service -Name "redis-local" -ErrorAction SilentlyContinue
    if ($redisService -and $redisService.Status -ne "Running") {
        Start-Service -Name "redis-local"
        Start-Sleep -Seconds 2
        $redisReady = Test-NetConnection -ComputerName $redisHost -Port $redisPort -WarningAction SilentlyContinue
    }
}

Write-Host ("MySQL reachable: " + $mysqlReady.TcpTestSucceeded + " (" + $mysqlHost + ":" + $mysqlPort + ")")
Write-Host ("Redis reachable: " + $redisReady.TcpTestSucceeded + " (" + $redisHost + ":" + $redisPort + ")")
Write-Host ("Using database: " + $databaseName)

mvn -gs .mvn\temp-settings.xml '-Dmaven.repo.local=D:\.projects\xunzhiyin\.m2repo' '-Dlocal.integration.enabled=true' '-Dsurefire.useFile=false' -Dtest=LocalMysqlFlowIntegrationTest test
