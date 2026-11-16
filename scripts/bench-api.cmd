@echo off
rem Run API benchmark with the project's portable Node (no global node needed)
rem Usage: scripts\bench-api.cmd https://127.0.0.1:8443 <API_KEY> /api/v1/metrics/latest 2000 20
setlocal
set "ROOT=%~dp0.."
set "PATH=%ROOT%\.tools\node;%PATH%"
if not exist "%ROOT%\.tmp" mkdir "%ROOT%\.tmp"
set "TEMP=%ROOT%\.tmp"
set "TMP=%ROOT%\.tmp"
"%ROOT%\.tools\node\node.exe" "%~dp0bench-api.mjs" %*
endlocal
