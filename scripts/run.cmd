@echo off
rem Start backend (bypasses PowerShell execution policy)
rem Usage: scripts\run.cmd   or   scripts\run.cmd -Interval 10
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1" %*
endlocal
