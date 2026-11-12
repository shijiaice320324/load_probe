# 在当前 PowerShell 会话中启用项目自带的便携 JDK 21 与 Maven
# 用法（注意前面有一个点和一个空格，表示 dot-source）：
#     . .\scripts\dev-env.ps1
# 之后即可直接使用 java / javac / mvn。

$root = Split-Path -Parent $PSScriptRoot
$env:JAVA_HOME = Join-Path $root '.tools\jdk-21.0.13+9'
$mavenHome = Join-Path $root '.tools\apache-maven-3.9.16'
$env:PATH = "$env:JAVA_HOME\bin;$mavenHome\bin;$env:PATH"

Write-Host "JAVA_HOME = $env:JAVA_HOME"
Write-Host "MAVEN_HOME = $mavenHome"
