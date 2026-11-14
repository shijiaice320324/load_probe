param(
    [switch]$Online
)

# 统一用 UTF-8 输出，避免重定向到文件后中文乱码
try {
    $OutputEncoding = [System.Text.Encoding]::UTF8
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
} catch {
    # 某些受限环境不允许改控制台编码，忽略即可
}

$root = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $root 'target\load-probe.jar'
$logs = Join-Path $root '.tmp'
New-Item -ItemType Directory -Force -Path $logs | Out-Null
Set-Location $root

# 读取配置，判断 HTTPS 端口（默认 8443）
$tlsPort = '8443'
$tlsEnabled = $true
$envFile = Join-Path $root 'config\local.env'
if (Test-Path $envFile) {
    foreach ($line in Get-Content $envFile) {
        if ($line -match '^LOADPROBE_TLS_PORT=(.+)$') { $tlsPort = $Matches[1].Trim() }
        if ($line -match '^LOADPROBE_TLS_ENABLED=(.+)$') { $tlsEnabled = ($Matches[1].Trim() -ne 'false') }
    }
}
$healthUrl = if ($tlsEnabled) { "https://127.0.0.1:$tlsPort/api/v1/health" } else { 'http://127.0.0.1:8080/api/v1/health' }

Write-Host '=== 1/4 检查数据库 ===' -ForegroundColor Cyan
& 'D:\pg\pgsql\bin\pg_isready.exe' -h 127.0.0.1 -p 5432 | Out-Host
if ($LASTEXITCODE -ne 0) {
    Write-Warning 'PostgreSQL 未就绪，请先执行 scripts\pg-start.cmd；构建会继续，但启动后可能连不上库。'
}

Write-Host '=== 2/4 停止旧的服务进程 ===' -ForegroundColor Cyan
# Windows 下运行中的 jar 会被锁定，必须先停进程，否则 mvn clean 无法删除 target\load-probe.jar
# 同时兼容旧版本还监听 8080 的情况
& "$PSScriptRoot\stop-app.ps1" -Ports 8443, 443, 8080
Start-Sleep -Seconds 2

Write-Host '=== 3/4 构建（含单元测试）===' -ForegroundColor Cyan
$env:JAVA_HOME = Join-Path $root '.tools\jdk-21.0.13+9'
$env:PATH = "$env:JAVA_HOME\bin;" + (Join-Path $root '.tools\apache-maven-3.9.16\bin') + ";$env:PATH"

$mvnArgs = @('-B', '-ntp', 'clean', 'package')
if (-not $Online) {
    $mvnArgs = @('-o') + $mvnArgs
}
& mvn @mvnArgs
if ($LASTEXITCODE -ne 0) {
    if (-not $Online) {
        Write-Warning '离线构建失败。若报错是「缺少依赖」，请改用：scripts\rebuild-backend.cmd -Online'
    }
    Write-Warning '若报错是「另一个程序正在使用此文件」，说明还有进程占用 jar，先执行 scripts\stop-app.cmd'
    Write-Error '构建失败，已中止，未重启后端。'
    exit 1
}
Write-Host ("构建成功，jar 时间：" + (Get-Item $jar).LastWriteTime) -ForegroundColor Green

Write-Host '=== 4/4 启动后端 ===' -ForegroundColor Cyan
Start-Process -FilePath 'powershell.exe' `
    -ArgumentList '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $PSScriptRoot 'run.ps1') `
    -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $logs 'backend.out.log') `
    -RedirectStandardError (Join-Path $logs 'backend.err.log') | Out-Null

for ($i = 0; $i -lt 90; $i++) {
    Start-Sleep -Seconds 1
    if ($tlsEnabled) {
        curl.exe -k -s -o NUL -m 2 $healthUrl 2>$null
    } else {
        curl.exe -s -o NUL -m 2 $healthUrl 2>$null
    }
    if ($LASTEXITCODE -eq 0) {
        Write-Host "后端已就绪（耗时 $i 秒）" -ForegroundColor Green
        $curlArgs = if ($tlsEnabled) { @('-k', '-s', $healthUrl) } else { @('-s', $healthUrl) }
        (& curl.exe @curlArgs) | Write-Host
        Write-Host ''
        Write-Host '访问：' -ForegroundColor Cyan
        Write-Host ("  后端  " + $healthUrl)
        Write-Host '  前端  http://127.0.0.1:5173  （需另开窗口跑 scripts\web-dev.cmd）'
        Write-Host ('  日志  ' + (Join-Path $root 'logs\load-probe.log'))
        exit 0
    }
}

Write-Error "后端在 90 秒内没有就绪，请查看 $(Join-Path $logs 'backend.out.log') 和 logs\load-probe.log"
exit 1
