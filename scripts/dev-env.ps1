# 在当前 PowerShell 会话中启用项目自带的便携 JDK 21、Maven 与 Node
# 用法（注意前面有一个点和一个空格，表示 dot-source）：
#     . .\scripts\dev-env.ps1
# 之后即可直接使用 java / javac / mvn / node / npm。

$root = Split-Path -Parent $PSScriptRoot
$env:JAVA_HOME = Join-Path $root '.tools\jdk-21.0.13+9'
$mavenHome = Join-Path $root '.tools\apache-maven-3.9.16'
$nodeHome = Join-Path $root '.tools\node'
$env:PATH = "$env:JAVA_HOME\bin;$mavenHome\bin;$nodeHome;$env:PATH"

# 本机用户临时目录不可写，Node/npm 相关工具改用项目内目录
$tmp = Join-Path $root '.tmp'
if (-not (Test-Path $tmp)) { New-Item -ItemType Directory -Force -Path $tmp | Out-Null }
$env:TEMP = $tmp
$env:TMP = $tmp
$env:npm_config_cache = Join-Path $root '.tools\npm-cache'

Write-Host "JAVA_HOME = $env:JAVA_HOME"
Write-Host "MAVEN_HOME = $mavenHome"
Write-Host "NODE_HOME  = $nodeHome"
