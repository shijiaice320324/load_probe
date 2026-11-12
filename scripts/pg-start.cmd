@echo off
rem Start local PostgreSQL
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0pg-start.ps1" %*
endlocal
