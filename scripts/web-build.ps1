# 构建前端静态产物到 web\dist
$root = Split-Path -Parent $PSScriptRoot
$web = Join-Path $root 'web'
$node = Join-Path $root '.tools\node'
$cache = Join-Path $root '.tools\npm-cache'
$tmp = Join-Path $root '.tmp'

New-Item -ItemType Directory -Force -Path $cache, $tmp | Out-Null
$env:PATH = "$node;$env:PATH"
$env:npm_config_cache = $cache
$env:TEMP = $tmp
$env:TMP = $tmp

Set-Location $web
if (-not (Test-Path (Join-Path $web 'node_modules'))) {
    Write-Host '首次运行，安装依赖…'
    & npm.cmd install --no-fund --no-audit
}
& npm.cmd run build
exit $LASTEXITCODE
