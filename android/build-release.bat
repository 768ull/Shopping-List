@echo off
cd /d "%~dp0"
where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle 9.6+ nao encontrado. Instale o Gradle ou abra o projeto no Android Studio.
  exit /b 1
)
gradle --version
if errorlevel 1 exit /b 1
gradle :app:assembleRelease
if errorlevel 1 exit /b 1
if exist "app\build\outputs\apk\release\app-release.apk" (
  echo APK: app\build\outputs\apk\release\app-release.apk
) else (
  echo O build terminou sem criar o APK esperado.
  exit /b 2
)
