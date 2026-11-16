# 生产模式构建：前端打包进后端 jar，然后用一个 HTTPS 地址访问（不再需要 Vite）
# 用法： scripts\package-full.cmd
param(
    [switch]$Online
)

$root = Split-Path -Parent $PSScriptRoot
try {
    $OutputEncoding = [System.Text.Encoding]::UTF8
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
} catch { }

Write-Host '=== 1/3 构建前端（web/dist）===' -ForegroundColor Cyan
& "$PSScriptRoot\web-build.ps1"
if ($LASTEXITCODE -ne 0) {
    Write-Error '前端构建失败，已中止。'
    exit 1
}

Write-Host '=== 2/3 构建后端并把前端打进 jar ===' -ForegroundColor Cyan
if ($Online) {
    & "$PSScriptRoot\rebuild-backend.ps1" -Online
} else {
    & "$PSScriptRoot\rebuild-backend.ps1"
}
if ($LASTEXITCODE -ne 0) {
    Write-Error '后端构建/启动失败，已中止。'
    exit 1
}

Write-Host '=== 3/3 校验页面与接口 ===' -ForegroundColor Cyan
$tlsPort = '8443'
$envFile = Join-Path $root 'config\local.env'
if (Test-Path $envFile) {
    foreach ($line in Get-Content $envFile) {
        if ($line -match '^LOADPROBE_TLS_PORT=(.+)$') { $tlsPort = $Matches[1].Trim() }
    }
}
$base = "https://127.0.0.1:$tlsPort"

curl.exe -k -s -o NUL -m 5 "$base/"
$pageOk = ($LASTEXITCODE -eq 0)
$hasApp = $false
if ($pageOk) {
    $html = (curl.exe -k -s -m 5 "$base/") | Out-String
    $hasApp = $html -match 'id="app"'
}
curl.exe -k -s -o NUL -m 5 "$base/api/v1/health"
$apiOk = ($LASTEXITCODE -eq 0)

Write-Host ''
Write-Host ("页面 /              : " + $(if ($hasApp) { 'OK' } else { '未检测到 index.html' }))
Write-Host ("接口 /api/v1/health : " + $(if ($apiOk) { 'OK' } else { '失败' }))
Write-Host ''
if ($hasApp -and $apiOk) {
    Write-Host "生产模式就绪，浏览器打开： $base" -ForegroundColor Green
    Write-Host '（前端与接口同源，无需 Vite，也无需 5173）'
    exit 0
}
Write-Error "校验未通过，请查看 logs\load-probe.log 与 .tmp\rebuild.out.log"
exit 1
