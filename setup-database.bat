@echo off
REM Seed the MediTrack database with schema and initial data
setlocal enabledelayedexpansion

set MYSQL_HOST=localhost
set MYSQL_PORT=3307
set MYSQL_USER=meditrack_user
set MYSQL_PASS=meditrack_pass
set DB_DIR=d:\Projects\CascadeProjects\meditrack-ai\database

echo.
echo ====================================================
echo MediTrack Database Setup
echo ====================================================
echo.

echo Creating database schemas...
for %%F in (%DB_DIR%\schema\*.sql) do (
    echo Executing: %%~nF
    mysql -h %MYSQL_HOST% -P %MYSQL_PORT% -u %MYSQL_USER% -p%MYSQL_PASS% < "%%F"
    if !ERRORLEVEL! neq 0 (
        echo Error executing %%~nF
        exit /b 1
    )
)

echo.
echo Seeding database with initial data...
for %%F in (%DB_DIR%\seed\*.sql) do (
    echo Seeding: %%~nF
    mysql -h %MYSQL_HOST% -P %MYSQL_PORT% -u %MYSQL_USER% -p%MYSQL_PASS% < "%%F"
    if !ERRORLEVEL! neq 0 (
        echo Error seeding %%~nF
        exit /b 1
    )
)

echo.
echo ====================================================
echo ✅ Database setup complete!
echo ====================================================
echo.

endlocal
