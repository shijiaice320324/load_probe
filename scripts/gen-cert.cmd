@echo off
rem Generate self-signed certificate for HTTPS (PKCS12 + PEM)
rem Usage: scripts\gen-cert.cmd            (reuse existing cert if present)
rem        scripts\gen-cert.cmd -Force   (regenerate)
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0gen-cert.ps1" %*
endlocal
