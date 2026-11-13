@echo off
rem Rebuild backend: stop old process -> mvn package -> start again
rem Usage: scripts\rebuild-backend.cmd         (offline build first)
rem        scripts\rebuild-backend.cmd -Online (force online build)
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0rebuild-backend.ps1" %*
endlocal
