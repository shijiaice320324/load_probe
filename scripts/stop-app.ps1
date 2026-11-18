param(
    # 支持 -Ports 8443,443 或 -Ports 8443 -Ports 443 两种写法
    [string[]]$Ports = @('8443', '443', '5173')
)

$targets = @()
foreach ($item in $Ports) {
    foreach ($part in ($item -split ',')) {
        $trimmed = $part.Trim()
        if ($trimmed -match '^\d+$') { $targets += [int]$trimmed }
    }
}

foreach ($port in ($targets | Sort-Object -Unique)) {
    $lines = netstat -ano | Select-String ":$port\s" | Select-String 'LISTENING'
    $pids = @()
    foreach ($line in $lines) {
        $parts = ($line.Line -replace '\s+', ' ').Trim().Split(' ')
        $pids += [int]$parts[-1]
    }
    $pids = $pids | Sort-Object -Unique
    if ($pids.Count -eq 0) {
        Write-Host "端口 $port 没有监听进程"
        continue
    }
    foreach ($processId in $pids) {
        try {
            Stop-Process -Id $processId -Force -ErrorAction Stop
            Write-Host "已停止端口 $port 的进程 (PID=$processId)"
        } catch {
            Write-Host "停止 PID=$processId 失败：$($_.Exception.Message)"
        }
    }
}
