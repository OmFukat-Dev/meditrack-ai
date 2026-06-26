@echo off
setlocal
cd /d "%~dp0frontend"

echo Checking frontend dependencies...
if not exist node_modules (
  echo Installing frontend dependencies...
  npm install
  if errorlevel 1 (
    echo Frontend dependency install failed.
    exit /b 1
  )
) else (
  echo Frontend dependencies already installed.
)

echo.
echo Starting MediTrack AI Frontend...
echo Frontend will be available at: http://localhost:3000
echo.
echo Features available:
echo - Beautiful modern login page with role selection
echo - Database-backed authentication system  
echo - Admin dashboard with staff management
echo - Doctor dashboard with real vital charts
echo - Report generation and call functionality
echo.

npm run dev
endlocal
