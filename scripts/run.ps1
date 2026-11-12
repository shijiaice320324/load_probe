﻿# 在当前 PowerShell 会话里加载配置并启动应用（手动 jar 方式）
# 用法： .\scripts\run.ps1            # 使用默认配置
#        .\scripts\run.ps1 -Interval 10
param(
    [int]$Interval
)

$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root 'config\local.env'

if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq '' -or $line.StartsWith('#')) { return }
        $idx = $line.IndexOf('=')
        if ($idx -lt 1) { return }
        $name = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim()
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    }
    Write-Host "已加载配置: $envFile"
} else {
    Write-Warning "未找到 $envFile，将使用 application.yml 中的默认值（数据库密码可能为空）"
}

# 使用项目内便携 JDK（与 mvn 构建用的是同一个）
$jdk = Join-Path $root '.tools\jdk-21.0.13+9'
if (Test-Path $jdk) {
    $env:JAVA_HOME = $jdk
    $env:PATH = "$jdk\bin;$env:PATH"
}

if ($Interval -gt 0) {
    $env:LOADPROBE_INTERVAL_SECONDS = $Interval
    Write-Host "采集间隔覆盖为 $Interval 秒"
}

if (-not $env:LOADPROBE_LOG_FILE) {
    $env:LOADPROBE_LOG_FILE = Join-Path $root 'logs\load-probe.log'
}

$jar = Join-Path $root 'target\load-probe.jar'
if (-not (Test-Path $jar)) {
    Write-Error "找不到 $jar，请先执行 mvn clean package"
    exit 1
}

# 本机 C:\Users\<用户>\AppData\Local\Temp 写入被拒绝，把 JVM 临时目录改到项目内
$tmp = Join-Path $root '.tmp'
New-Item -ItemType Directory -Force -Path $tmp | Out-Null

Write-Host "启动: java -jar $jar"
Write-Host "日志: $env:LOADPROBE_LOG_FILE"
Set-Location $root
& java "-Djava.io.tmpdir=$tmp" -jar $jar
