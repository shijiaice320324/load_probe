@echo off
rem Build frontend + backend into one jar, then restart (production mode, HTTPS only)
rem Usage: scripts\package-full.cmd
rem        scripts\package-full.cmd -Online
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0package-full.ps1" %*
endlocal
