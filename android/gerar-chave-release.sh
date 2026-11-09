#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

if ! command -v keytool >/dev/null 2>&1; then
  echo "keytool não encontrado. Instale o JDK 17 (por exemplo: sudo apt install openjdk-17-jdk)." >&2
  exit 1
fi

if [[ -s signing/shopping-list-release.jks ]]; then
  echo "Já existe signing/shopping-list-release.jks. Remova-o se quiser gerar outra chave." >&2
  exit 0
fi

mkdir -p signing
read -r -p "Senha do keystore: " STOREPASS
if [[ -z "$STOREPASS" ]]; then
  echo "Senha obrigatória." >&2
  exit 1
fi
read -r -p "Senha da chave [Enter = mesma senha]: " KEYPASS
KEYPASS="${KEYPASS:-$STOREPASS}"

keytool -genkeypair -v \
  -keystore signing/shopping-list-release.jks \
  -alias shoppinglist \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$STOREPASS" -keypass "$KEYPASS" \
  -dname "CN=Shopping Lists, O=Shopping Lists, C=BR"

cat > signing/signing.properties <<EOF
storeFile=shopping-list-release.jks
storePassword=$STOREPASS
keyAlias=shoppinglist
keyPassword=$KEYPASS
EOF
chmod 600 signing/signing.properties

base64 signing/shopping-list-release.jks | tr -d '\n' > signing/keystore-base64.txt

cat <<'FIM'

Chave criada: signing/shopping-list-release.jks
Base64 do Secret: signing/keystore-base64.txt
Propriedades locais: signing/signing.properties

Próximos passos no GitHub:
  1. Settings, Secrets and variables, Actions, New repository secret
  2. SHOPPING_KEYSTORE_BASE64 = conteúdo integral de signing/keystore-base64.txt
  3. SHOPPING_KEYSTORE_PASSWORD = a senha do keystore
  4. SHOPPING_KEY_ALIAS = shoppinglist
  5. SHOPPING_KEY_PASSWORD = a senha da chave
  6. Actions, Build Shopping Lists APK, Run workflow com version_code 13 e version_name 1.0.12

Guarde uma cópia da pasta signing em local seguro. Ela não é enviada ao Git.
FIM
