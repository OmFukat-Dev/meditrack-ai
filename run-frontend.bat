@echo off
setlocal
cd /d "%~dp0frontend"

set "NPM_CMD="
where npm >nul 2>nul
if %errorlevel% equ 0 (
  set "NPM_CMD=npm"
)

if "%NPM_CMD%"=="" (
  echo ERROR: npm was not found. Please install Node.js and ensure npm is in PATH.
  pause
  exit /b 1
)

echo Checking frontend dependencies...
if not exist node_modules (
  echo Installing frontend dependencies...
  call %NPM_CMD% install
  if errorlevel 1 (
    echo ERROR: Frontend dependency install failed.
    pause
    exit /b 1
  )
) else (
  echo Frontend dependencies are already installed.
)

echo.
echo Starting MediTrack AI Frontend...
echo Frontend will be available at: http://localhost:3000
echo.
call %NPM_CMD% run dev
endlocal
