@echo off
setlocal
set "APP_HOME=%~dp0"
set "GRADLE_VERSION=9.3.1"
if "%GRADLE_USER_HOME%"=="" set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "BOOTSTRAP_DIR=%GRADLE_USER_HOME%\hisn-bootstrap"
set "GRADLE_HOME=%BOOTSTRAP_DIR%\gradle-%GRADLE_VERSION%"
set "GRADLE_BIN=%GRADLE_HOME%\bin\gradle.bat"
set "ZIP=%BOOTSTRAP_DIR%\gradle-%GRADLE_VERSION%-bin.zip"
if exist "%GRADLE_BIN%" goto runGradle
if not exist "%BOOTSTRAP_DIR%" mkdir "%BOOTSTRAP_DIR%"
if not exist "%ZIP%" (
  echo Downloading official Gradle %GRADLE_VERSION% distribution...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP%.part'"
  if errorlevel 1 exit /b 1
  move /y "%ZIP%.part" "%ZIP%" >nul
)
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%ZIP%' -DestinationPath '%BOOTSTRAP_DIR%' -Force"
if errorlevel 1 exit /b 1
:runGradle
call "%GRADLE_BIN%" -p "%APP_HOME%" %*
exit /b %ERRORLEVEL%
