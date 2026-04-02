@echo off
setlocal

set "ROOT_DIR=%~dp0"

echo Starting backend...
start "Invision Main API" cmd /k ""%ROOT_DIR%invision-main-api\start-main-api.bat""

echo Starting Flutter web...
start "Invision Flutter Web" powershell -ExecutionPolicy Bypass -File "%ROOT_DIR%invision_app\run_web_auto_stop.ps1" -Device edge

echo.
echo Demo startup launched.
echo Backend window: Invision Main API
echo Frontend window: Invision Flutter Web
