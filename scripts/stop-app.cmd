@echo off
rem Stop backend (8080) and frontend (5173) by port
rem Usage: scripts\stop-app.cmd
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-app.ps1" %*
endlocal
