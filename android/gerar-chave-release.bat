@echo off
setlocal
cd /d "%~dp0"

set "KEYTOOL=keytool"
where keytool >nul 2>nul
if errorlevel 1 (
  if exist "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" (
    set "KEYTOOL=C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
  ) else (
    echo keytool nao encontrado. Instale o JDK 17 ou abra o Android Studio uma vez.
    pause
    exit /b 1
  )
)

if exist signing\shopping-list-release.jks (
  echo Ja existe signing\shopping-list-release.jks. Apague o arquivo se quiser gerar outra chave.
  pause
  exit /b 0
)

if not exist signing mkdir signing

set "STOREPASS="
set /p STOREPASS=Digite a senha do keystore (use letras e numeros): 
if "%STOREPASS%"=="" (
  echo Senha obrigatoria.
  pause
  exit /b 1
)
set "KEYPASS="
set /p KEYPASS=Digite a senha da chave [Enter = mesma senha]: 
if "%KEYPASS%"=="" set "KEYPASS=%STOREPASS%"

"%KEYTOOL%" -genkeypair -v -keystore signing\shopping-list-release.jks -alias shoppinglist -keyalg RSA -keysize 4096 -validity 10000 -storepass "%STOREPASS%" -keypass "%KEYPASS%" -dname "CN=Shopping Lists, O=Shopping Lists, C=BR"
if errorlevel 1 (
  echo Falha ao criar a chave.
  pause
  exit /b 1
)

> signing\signing.properties (
  echo storeFile=shopping-list-release.jks
  echo storePassword=%STOREPASS%
  echo keyAlias=shoppinglist
  echo keyPassword=%KEYPASS%
)

powershell -NoProfile -Command "[IO.File]::WriteAllText('signing\keystore-base64.txt', [Convert]::ToBase64String([IO.File]::ReadAllBytes('signing\shopping-list-release.jks')))"

echo.
echo Chave criada: signing\shopping-list-release.jks
echo Base64 do Secret: signing\keystore-base64.txt
echo Propriedades locais: signing\signing.properties
echo.
echo Proximos passos no GitHub:
echo   1. Settings, Secrets and variables, Actions, New repository secret
echo   2. SHOPPING_KEYSTORE_BASE64 = conteudo integral de signing\keystore-base64.txt
echo   3. SHOPPING_KEYSTORE_PASSWORD = a senha do keystore
echo   4. SHOPPING_KEY_ALIAS = shoppinglist
echo   5. SHOPPING_KEY_PASSWORD = a senha da chave
echo   6. Actions, Build Shopping Lists APK, Run workflow com version_code 13 e version_name 1.0.12
echo.
echo Guarde uma copia da pasta signing em local seguro. Ela nao e enviada ao Git.
pause
endlocal
