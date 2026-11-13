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

Write-Host '=== 1/4 检查数据库 ===' -ForegroundColor Cyan
& 'D:\pg\pgsql\bin\pg_isready.exe' -h 127.0.0.1 -p 5432 | Out-Host
if ($LASTEXITCODE -ne 0) {
    Write-Warning 'PostgreSQL 未就绪，请先执行 scripts\pg-start.cmd；构建会继续，但启动后可能连不上库。'
}

Write-Host '=== 2/4 停止占用 8080 的旧进程 ===' -ForegroundColor Cyan
# Windows 下运行中的 jar 会被锁定，必须先停进程，否则 mvn clean 无法删除 target\load-probe.jar
& "$PSScriptRoot\stop-app.ps1" -Ports 8080
Start-Sleep -Seconds 2

Write-Host '=== 3/4 构建（含单元测试）===' -ForegroundColor Cyan
$env:JAVA_HOME = Join-Path $root '.tools\jdk-21.0.13+9'
$env:PATH = "$env:JAVA_HOME\bin;" + (Join-Path $root '.tools\apache-maven-3.9.16\bin') + ";$env:PATH"

$mvnArgs = @('-B', '-ntp', 'clean', 'package')
if (-not $Online) {
    $mvnArgs = @('-o') + $mvnArgs
}
& mvn @mvnArgs
if ($LASTEXITCODE -ne 0 -and -not $Online) {
    Write-Warning '离线构建失败（可能缺少依赖），改为联网重试…'
    & mvn -B -ntp clean package
}
if ($LASTEXITCODE -ne 0) {
    Write-Error '构建失败，已中止，未重启后端。请查看上方 Maven 输出。'
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
    curl.exe -s -o NUL -m 2 http://127.0.0.1:8080/api/v1/health 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "后端已就绪（耗时 $i 秒）" -ForegroundColor Green
        (curl.exe -s http://127.0.0.1:8080/api/v1/health) | Write-Host
        Write-Host ''
        Write-Host '访问：' -ForegroundColor Cyan
        Write-Host '  后端  http://127.0.0.1:8080/api/v1/health'
        Write-Host '  前端  http://127.0.0.1:5173  （需另开窗口跑 scripts\web-dev.cmd）'
        Write-Host ('  日志  ' + (Join-Path $root 'logs\load-probe.log'))
        exit 0
    }
}

Write-Error "后端在 90 秒内没有就绪，请查看 $(Join-Path $logs 'backend.out.log') 和 logs\load-probe.log"
exit 1
