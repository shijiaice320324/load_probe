# 启动本机便携版 PostgreSQL（脱离当前终端独立常驻）
$pgbin = 'D:\pg\pgsql\bin'
$data = 'D:\pgdata'
$logFile = 'D:\pg\log\postgres.log'

& "$pgbin\pg_isready.exe" -h 127.0.0.1 -p 5432 *> $null
if ($LASTEXITCODE -eq 0) {
    Write-Host "PostgreSQL 已在运行 (127.0.0.1:5432)"
    exit 0
}

Start-Process -FilePath "$pgbin\postgres.exe" -ArgumentList '-D', $data `
    -WindowStyle Hidden `
    -RedirectStandardOutput 'D:\pg\log\postgres-stdout.log' `
    -RedirectStandardError 'D:\pg\log\postgres-stderr.log'

for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    & "$pgbin\pg_isready.exe" -h 127.0.0.1 -p 5432 *> $null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "PostgreSQL 已启动 (127.0.0.1:5432)，日志: $logFile"
        exit 0
    }
}
Write-Error "PostgreSQL 启动超时，请查看 $logFile"
exit 1
