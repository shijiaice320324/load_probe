@echo off
rem Start frontend dev server
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0web-dev.ps1" %*
endlocal
