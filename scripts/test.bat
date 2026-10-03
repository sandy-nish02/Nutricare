@echo off
setlocal
cd /d "%~dp0\.."
where mvn >nul 2>nul
if not errorlevel 1 (
  call mvn clean test package
) else if exist ".tools\apache-maven-3.9.9\bin\mvn.cmd" (
  call ".tools\apache-maven-3.9.9\bin\mvn.cmd" clean test package
) else (
  echo Run run.bat once to prepare Maven, then run this test script again.
  exit /b 1
)
