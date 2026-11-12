# 启动前端开发服务器（http://127.0.0.1:5173）
# 依赖后端已在 8080 运行：先执行 ..\scripts\run.ps1
$root = Split-Path -Parent $PSScriptRoot
$web = Join-Path $root 'web'
$node = Join-Path $root '.tools\node'
$cache = Join-Path $root '.tools\npm-cache'
$tmp = Join-Path $root '.tmp'

New-Item -ItemType Directory -Force -Path $cache, $tmp | Out-Null
$env:PATH = "$node;$env:PATH"
$env:npm_config_cache = $cache
# 本机用户临时目录不可写，npm/vite/esbuild 都改用项目内目录
$env:TEMP = $tmp
$env:TMP = $tmp

Set-Location $web
if (-not (Test-Path (Join-Path $web 'node_modules'))) {
    Write-Host '首次运行，安装依赖…'
    & npm.cmd install --no-fund --no-audit
}

# 启动前先探一下后端，避免页面起来后满屏 connect ECONNREFUSED
$backendOk = $false
try {
    $probe = Invoke-WebRequest -Uri 'http://127.0.0.1:8080/api/v1/health' -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop
    $backendOk = $true
    Write-Host "后端在线（HTTP $($probe.StatusCode)）"
} catch {
    Write-Host ''
    Write-Warning '后端 127.0.0.1:8080 未响应！页面上的接口会报 connect ECONNREFUSED。'
    Write-Warning '请另开一个终端窗口，在项目根目录执行：scripts\run.cmd'
    Write-Host ''
}
if (-not $backendOk) {
    Write-Host '仍要继续启动前端？3 秒后继续（Ctrl+C 可取消）…'
    Start-Sleep -Seconds 3
}

& npm.cmd run dev -- --host 127.0.0.1 --port 5173
