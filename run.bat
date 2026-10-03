@echo off
setlocal
cd /d "%~dp0"
if not defined DB_MODE set "DB_MODE=sqlserver"
if /I "%DB_MODE%"=="sqlserver" (
  if defined DB_PASSWORD (
    echo Connecting to SQL Server with SQL login "%DB_USER%" ^(default sa^).
  ) else (
    echo Connecting to SQL Server with your Windows account ^(set DB_USER and DB_PASSWORD to use a SQL login^).
  )
)
rem Microsoft's Windows-authentication library for the SQL Server JDBC driver ships with the project.
set "MAVEN_OPTS=%MAVEN_OPTS% -Djava.library.path="%CD%\sqljdbc_13.4\enu\auth\x64""
set "MAVEN_VERSION=3.9.9"
set "LOCAL_MAVEN=%CD%\.tools\apache-maven-%MAVEN_VERSION%\bin\mvn.cmd"

where java >nul 2>nul
if errorlevel 1 (
  echo Java 17 or newer is required. Install a JDK from https://adoptium.net/
  pause
  exit /b 1
)

where mvn >nul 2>nul
if not errorlevel 1 (
  set "MAVEN_CMD=mvn"
  goto start
)

if exist "%LOCAL_MAVEN%" (
  set "MAVEN_CMD=%LOCAL_MAVEN%"
  goto start
)

echo Maven is not installed. Downloading a private project copy once...
if not exist ".tools" mkdir ".tools"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $u='https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip'; Invoke-WebRequest -Uri $u -OutFile '.tools\maven.zip'; Expand-Archive -Path '.tools\maven.zip' -DestinationPath '.tools' -Force; Remove-Item '.tools\maven.zip'"
if errorlevel 1 (
  echo Maven download failed. Check the internet connection or install Maven 3.9+ manually.
  pause
  exit /b 1
)
set "MAVEN_CMD=%LOCAL_MAVEN%"

:start
echo Starting NutriCare at http://localhost:8080/nutricare/
call "%MAVEN_CMD%" jetty:run
if errorlevel 1 pause
