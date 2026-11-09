#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

SIGNING_PROPERTIES="signing/signing.properties"
if [[ ! -s "$SIGNING_PROPERTIES" ]]; then
  echo "Assinatura Release não configurada. Crie signing/signing.properties e coloque o keystore privado indicado em storeFile dentro de signing/." >&2
  exit 1
fi

STORE_FILE="$(sed -n 's/^[[:space:]]*storeFile[[:space:]]*=[[:space:]]*//p' "$SIGNING_PROPERTIES" | head -n 1)"
STORE_FILE="${STORE_FILE:-shopping-list-release.jks}"
if [[ "$STORE_FILE" = /* ]]; then
  KEYSTORE="$STORE_FILE"
else
  KEYSTORE="signing/$STORE_FILE"
fi
if [[ ! -s "$KEYSTORE" ]]; then
  echo "Keystore não encontrado: $KEYSTORE" >&2
  exit 1
fi

if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle 9.6.0 não encontrado. Instale essa versão ou abra android/ no Android Studio." >&2
  exit 1
fi
gradle --version
gradle --no-daemon :app:assembleRelease

APK="app/build/outputs/apk/release/app-release.apk"
if [[ ! -s "$APK" ]]; then
  echo "O build terminou sem criar $APK" >&2
  exit 2
fi

APKSIGNER=""
if command -v apksigner >/dev/null 2>&1; then
  APKSIGNER="$(command -v apksigner)"
elif [[ -n "${ANDROID_HOME:-}" && -x "$ANDROID_HOME/build-tools/36.0.0/apksigner" ]]; then
  APKSIGNER="$ANDROID_HOME/build-tools/36.0.0/apksigner"
elif [[ -n "${ANDROID_SDK_ROOT:-}" && -x "$ANDROID_SDK_ROOT/build-tools/36.0.0/apksigner" ]]; then
  APKSIGNER="$ANDROID_SDK_ROOT/build-tools/36.0.0/apksigner"
else
  echo "apksigner não encontrado. Instale Android SDK Build-Tools 36.0.0." >&2
  exit 3
fi

"$APKSIGNER" verify --verbose --print-certs "$APK" | tee app/build/outputs/apk/release/signature-report.txt
for scheme in v1 v2 v3; do
  if ! grep -Eiq "Verified using ${scheme} scheme.*: true" app/build/outputs/apk/release/signature-report.txt; then
    echo "A assinatura ${scheme^^} não foi confirmada." >&2
    exit 4
  fi
done
if [[ ! -s "$APK.idsig" ]]; then
  echo "A assinatura V4 (.idsig) não foi gerada." >&2
  exit 5
fi

python3 - "$APK" <<'PY'
import sys
from zipfile import ZipFile, ZIP_STORED
apk = sys.argv[1]
with ZipFile(apk) as archive:
    broken = archive.testzip()
    if broken:
        raise SystemExit(f"APK corrompido: {broken}")
    native = [item for item in archive.infolist() if item.filename.startswith("lib/") and item.filename.endswith(".so")]
    if not native:
        raise SystemExit("APK sem bibliotecas nativas.")
    stored = [item.filename for item in native if item.compress_type == ZIP_STORED]
    if stored:
        raise SystemExit("Bibliotecas .so sem compressão: " + ", ".join(stored[:5]))
PY

if command -v sha256sum >/dev/null 2>&1; then
  sha256sum "$APK"
fi
echo "APK Release validado: $APK"
echo "Assinatura V4: $APK.idsig"
