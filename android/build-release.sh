#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle 9.6+ não encontrado. Instale o Gradle ou abra o projeto no Android Studio." >&2
  exit 1
fi
gradle --version
gradle :app:assembleRelease
APK="app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "$APK" ]]; then
  echo "O build terminou sem criar $APK" >&2
  exit 2
fi
if command -v apksigner >/dev/null 2>&1; then
  apksigner verify --verbose "$APK"
else
  echo "APK criado; instale Android SDK Build-Tools para validar as assinaturas com apksigner."
fi
echo "APK: $APK"
