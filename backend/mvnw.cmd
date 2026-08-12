@echo off
setlocal
set MAVEN_OPTS=-Xms128m -Xmx768m -XX:MaxMetaspaceSize=384m

REM Try multiple Maven paths in order of preference
set MAVEN_PATH1=D:\Program Files\Apache\apache-maven-3.9.13\bin\mvn.cmd
set MAVEN_PATH2=C:\Users\fukat\.m2\wrapper\dists\apache-maven-3.8.4-bin\52ccbt68d252mdldqsfsn03jlf\apache-maven-3.8.4\bin\mvn.cmd

REM Check first Maven path
if exist "%MAVEN_PATH1%" (
    echo Using Maven at: D:\Program Files\Apache\apache-maven-3.9.13
    call "%MAVEN_PATH1%" %*
    goto :end
)

REM Check second Maven path
if exist "%MAVEN_PATH2%" (
    echo Using Maven at: C:\Users\fukat\.m2\wrapper\dists\apache-maven-3.8.4-bin\52ccbt68d252mdldqsfsn03jlf\apache-maven-3.8.4
    call "%MAVEN_PATH2%" %*
    goto :end
)

REM Try system Maven in PATH
where mvn >nul 2>nul
if %errorlevel% equ 0 (
    echo Using system Maven
    call mvn %*
    goto :end
)

REM No Maven found
echo ERROR: Maven not found in any known location
echo Please check Maven installation
echo Tried:
echo - %MAVEN_PATH1%
echo - %MAVEN_PATH2%
echo - System PATH
exit /b 1

:end
