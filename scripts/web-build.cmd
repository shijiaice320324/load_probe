@echo off
rem Build frontend into web\dist
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0web-build.ps1" %*
endlocal
