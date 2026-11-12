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
& npm.cmd run dev -- --host 127.0.0.1 --port 5173
