@echo off
setlocal
cd /d "%~dp0"

if not exist "signing\signing.properties" (
  echo Assinatura Release nao configurada. Crie signing\signing.properties e coloque o keystore privado dentro de signing\.
  exit /b 1
)
set "KEYSTORE=shopping-list-release.jks"
for /f "tokens=1,* delims==" %%A in ('findstr /b "storeFile=" "signing\signing.properties"') do set "KEYSTORE=%%B"
if not exist "signing\%KEYSTORE%" (
  echo Keystore nao encontrado: signing\%KEYSTORE%
  exit /b 1
)

where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle 9.6.0 nao encontrado. Instale essa versao ou abra android\ no Android Studio.
  exit /b 1
)
gradle --version
if errorlevel 1 exit /b 1
gradle --no-daemon :app:assembleRelease
if errorlevel 1 exit /b 1

if not exist "app\build\outputs\apk\release\app-release.apk" (
  echo O build terminou sem criar o APK esperado.
  exit /b 2
)
where apksigner >nul 2>nul
if errorlevel 1 (
  echo apksigner nao encontrado. Adicione Android SDK Build-Tools 36.0.0 ao PATH.
  exit /b 3
)
apksigner verify --verbose --print-certs "app\build\outputs\apk\release\app-release.apk"
if errorlevel 1 exit /b 4
if not exist "app\build\outputs\apk\release\app-release.apk.idsig" (
  echo A assinatura V4 .idsig nao foi gerada.
  exit /b 5
)
echo APK Release validado: app\build\outputs\apk\release\app-release.apk
