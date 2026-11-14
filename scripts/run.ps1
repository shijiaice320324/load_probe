# 在当前 PowerShell 会话里加载配置并启动应用（手动 jar 方式）
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

# ---------- HTTPS 相关检查 ----------
$tlsEnabled = ($env:LOADPROBE_TLS_ENABLED -eq 'true')
if ($tlsEnabled) {
    $store = $env:LOADPROBE_TLS_KEYSTORE
    if (-not $store) { $store = 'certs/loadprobe.p12' }
    $storePath = if ([System.IO.Path]::IsPathRooted($store)) { $store } else { Join-Path $root $store }
    if (-not (Test-Path $storePath)) {
        Write-Error "未找到证书库 $storePath，请先执行 scripts\gen-cert.cmd 生成自签证书"
        exit 1
    }

    # 端口规则：显式配置优先；否则 8443 空闲用 8443，被占用则退到 443
    function Test-PortFree([int]$port) {
        $listener = $null
        try {
            $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $port)
            $listener.Start()
            return $true
        } catch {
            return $false
        } finally {
            if ($listener) { $listener.Stop() }
        }
    }

    if (-not $env:LOADPROBE_TLS_PORT) {
        if (Test-PortFree 8443) {
            $env:LOADPROBE_TLS_PORT = '8443'
        } elseif (Test-PortFree 443) {
            $env:LOADPROBE_TLS_PORT = '443'
        } else {
            Write-Error '8443 与 443 均被占用，无法启动 HTTPS 服务'
            exit 1
        }
    }
    Write-Host "HTTPS 已启用，访问地址: https://127.0.0.1:$($env:LOADPROBE_TLS_PORT)"
} else {
    Write-Host 'HTTPS 未启用（LOADPROBE_TLS_ENABLED=false），以 HTTP 方式启动'
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
