$ErrorActionPreference = "Stop"

if (-not $env:JAVA_HOME) {
    throw "JAVA_HOME is not set. Please point it to JDK 21 before running this script."
}

$mysqlHost = if ($env:LOCAL_MYSQL_HOST) { $env:LOCAL_MYSQL_HOST } else { "127.0.0.1" }
$mysqlPort = if ($env:LOCAL_MYSQL_PORT) { [int]$env:LOCAL_MYSQL_PORT } else { 3306 }
$redisHost = if ($env:LOCAL_REDIS_HOST) { $env:LOCAL_REDIS_HOST } else { "127.0.0.1" }
$redisPort = if ($env:LOCAL_REDIS_PORT) { [int]$env:LOCAL_REDIS_PORT } else { 6379 }

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
Write-Host ("Using database: " + ($(if ($env:LOCAL_MYSQL_DB) { $env:LOCAL_MYSQL_DB } else { "campus_reco" })))

mvn -gs .mvn\temp-settings.xml '-Dmaven.repo.local=D:\.projects\xunzhiyin\xunzhiyin\.m2repo' '-Dlocal.integration.enabled=true' -Dtest=LocalMysqlFlowIntegrationTest test
