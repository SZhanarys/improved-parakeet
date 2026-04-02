@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "ENV_FILE=%SCRIPT_DIR%.env.local"

if not exist "%ENV_FILE%" (
  echo Missing %ENV_FILE%
  echo Copy .env.local.example to .env.local and put your real AI_API_KEY there.
  exit /b 1
)

for /f "usebackq tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
  set "KEY=%%A"
  set "VALUE=%%B"
  if defined KEY (
    if not "!KEY:~0,1!"=="#" (
      set "!KEY!=!VALUE!"
    )
  )
)

echo Starting Main API on port 9000...
call "%SCRIPT_DIR%gradlew.bat" bootRun
