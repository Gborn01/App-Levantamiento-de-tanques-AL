#!/usr/bin/env bash
# Descarga las herramientas para compilar SIN Android Studio (una sola vez, ~200 MB).
# Todas son oficiales/libres: Kotlin (JetBrains), bundletool (Google: incluye D8 y apksig),
# build-tools Android compilados para Linux x86_64, y android.jar (API 35).
# Requiere: Linux x86_64 (o WSL en Windows), Java 17+, curl, unzip, zip.
set -euo pipefail
cd "$(dirname "$0")/.."
T="${ANDROID_TOOLS:-$PWD/toolchain}"
mkdir -p "$T"; cd "$T"
echo "Descargando en $T"
curl -fL -o kotlin.zip https://github.com/JetBrains/kotlin/releases/download/v2.0.21/kotlin-compiler-2.0.21.zip
unzip -oq kotlin.zip && rm kotlin.zip
curl -fL -o bundletool.jar https://github.com/google/bundletool/releases/download/1.17.2/bundletool-all-1.17.2.jar
curl -fL -o sdk.zip https://github.com/lzhiyong/android-sdk-tools/releases/download/35.0.2/android-sdk-tools-static-x86_64.zip
unzip -oq sdk.zip -d sdk && rm sdk.zip && chmod +x sdk/build-tools/* sdk/platform-tools/*
cp sdk/build-tools/aapt2 aapt2
curl -fL -o android-35.jar https://raw.githubusercontent.com/Sable/android-platforms/master/android-35/android.jar
echo "Compilando firmador..."
kotlinc/bin/kotlinc "$OLDPWD/tools/signer/Signer.kt" "$OLDPWD/tools/signer/Verify.kt" -cp bundletool.jar -d signer.jar
echo "Listo. Ahora:  ./build.sh debug   o   ./build.sh release"
