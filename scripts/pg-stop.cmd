@echo off
rem Stop local PostgreSQL
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0pg-stop.ps1" %*
endlocal
