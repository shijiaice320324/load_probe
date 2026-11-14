param(
    [int]$Days = 825,
    [string]$Password = '',
    [string]$Alias = 'loadprobe',
    [switch]$Force
)

# 生成本机自签证书（PKCS12 + PEM 公钥），供 Spring Boot HTTPS 使用。
# 用法： scripts\gen-cert.cmd
#        scripts\gen-cert.cmd -Days 365 -Force
#        scripts\gen-cert.cmd -Password 你的密码

$root = Split-Path -Parent $PSScriptRoot
$certDir = Join-Path $root 'certs'
$p12 = Join-Path $certDir 'loadprobe.p12'
$crt = Join-Path $certDir 'loadprobe.crt'
$envFile = Join-Path $root 'config\local.env'
New-Item -ItemType Directory -Force -Path $certDir | Out-Null

if ((Test-Path $p12) -and -not $Force) {
    Write-Warning "证书已存在：$p12"
    Write-Warning '如需重新生成（例如即将过期），请加 -Force 参数。'
    exit 1
}

if (-not $Password) {
    $Password = 'lp_' + ([guid]::NewGuid().ToString('N').Substring(0, 20))
}

$keytool = Join-Path $root '.tools\jdk-21.0.13+9\bin\keytool.exe'
if (-not (Test-Path $keytool)) {
    $keytool = 'keytool'
}

$dname = 'CN=localhost, OU=load_probe, O=local, L=Shanghai, ST=Shanghai, C=CN'

Write-Host '=== 生成密钥对与自签证书 ===' -ForegroundColor Cyan
& $keytool -genkeypair `
    -alias $Alias `
    -keyalg RSA -keysize 2048 -sigalg SHA256withRSA `
    -dname $dname `
    -ext 'SAN=dns:localhost,ip:127.0.0.1' `
    -validity $Days `
    -keystore $p12 -storetype PKCS12 `
    -storepass $Password -keypass $Password
if ($LASTEXITCODE -ne 0) {
    Write-Error '生成失败，请检查上方 keytool 输出。'
    exit 1
}

Write-Host '=== 导出 PEM 公钥（供浏览器/系统导入信任）===' -ForegroundColor Cyan
& $keytool -exportcert -alias $Alias -keystore $p12 -storepass $Password -rfc -file $crt | Out-Null

# 把口令写回 config/local.env，避免每次手填
if (Test-Path $envFile) {
    $lines = Get-Content $envFile | Where-Object { $_ -notmatch '^LOADPROBE_TLS_' }
    $lines += "LOADPROBE_TLS_ENABLED=true"
    $lines += "LOADPROBE_TLS_PORT=8443"
    $lines += "LOADPROBE_TLS_KEYSTORE=certs/loadprobe.p12"
    $lines += "LOADPROBE_TLS_KEYSTORE_PASSWORD=$Password"
    $lines += "LOADPROBE_TLS_KEY_ALIAS=$Alias"
    # 必须写无 BOM 的 UTF-8，否则首行的 key 名会带上 BOM 字符导致读取失败
    [System.IO.File]::WriteAllLines($envFile, $lines, (New-Object System.Text.UTF8Encoding($false)))
    Write-Host "已写入配置：$envFile" -ForegroundColor Green
} else {
    Write-Warning "未找到 $envFile，请手动把下面两项写入配置："
    Write-Host "LOADPROBE_TLS_KEYSTORE=certs/loadprobe.p12"
    Write-Host "LOADPROBE_TLS_KEYSTORE_PASSWORD=$Password"
}

Write-Host ''
Write-Host '=== 完成 ===' -ForegroundColor Green
Write-Host "证书库    : $p12"
Write-Host "公钥(PEM) : $crt"
Write-Host "别名       : $Alias"
Write-Host "有效期     : $Days 天（至 $((Get-Date).AddDays($Days).ToString('yyyy-MM-dd'))）"
Write-Host "口令       : $Password"
Write-Host ''
Write-Host '下一步：scripts\rebuild-backend.cmd 重启后端，然后访问 https://127.0.0.1:8443'
Write-Host '浏览器首次访问会提示证书不受信任，点「高级 → 继续前往」即可，或导入上面的 PEM 到受信任根证书。'
