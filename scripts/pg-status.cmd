@echo off
rem Show local PostgreSQL status
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0pg-status.ps1" %*
endlocal
