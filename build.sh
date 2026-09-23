#!/usr/bin/env bash
# =============================================================================
#  Compilación directa de la APK SIN Gradle (solo herramientas oficiales):
#    aapt2 (recursos) · kotlinc (Kotlin) · D8 (dex) · zipalign · apksig (firma)
#
#  Uso:   ./build.sh debug      → dist/LevantamientoTanques-1.0.0-debug.apk
#         ./build.sh release    → dist/LevantamientoTanques-1.0.0-release.apk
#         ./build.sh test       → pruebas de lógica (JVM)
#
#  Requiere la carpeta de herramientas (ver README, "Compilar sin Android Studio"):
#    ANDROID_TOOLS (por defecto ./toolchain, se instala con tools/setup-toolchain.sh) con: android-35.jar, bundletool.jar,
#    kotlinc/, sdk/build-tools/zipalign, signer.jar
#  Con Android Studio también puede compilar con Gradle:  ./gradlew assembleDebug
# =============================================================================
set -euo pipefail
cd "$(dirname "$0")"
VARIANT="${1:-debug}"
T="${ANDROID_TOOLS:-$PWD/toolchain}"
APP_ID="com.jarabaimport.levantamiento"
VERSION_CODE=3
VERSION_NAME="1.0.2"
MIN_SDK=26
TARGET_SDK=34
ANDROID_JAR="$T/android-35.jar"
KOTLINC="$T/kotlinc/bin/kotlinc"
STDLIB="$T/kotlinc/lib/kotlin-stdlib.jar"
BUNDLETOOL="$T/bundletool.jar"
SRC="app/src/main/java"

for f in "$ANDROID_JAR" "$KOTLINC" "$BUNDLETOOL" "$T/signer.jar" "$T/sdk/build-tools/zipalign"; do
  [ -e "$f" ] || { echo "Falta $f (configure ANDROID_TOOLS)"; exit 1; }
done

if [ "$VARIANT" = "test" ]; then
  OUT=build/manual/test; rm -rf "$OUT"; mkdir -p "$OUT"
  echo "==> Compilando lógica + pruebas (JVM)"
  "$KOTLINC" $(find "$SRC/com/jarabaimport/levantamiento/domain" "$SRC/com/jarabaimport/levantamiento/export/CsvExporter.kt" \
      "$SRC/com/jarabaimport/levantamiento/data/db/ClientEntity.kt" "$SRC/com/jarabaimport/levantamiento/data/db/TankEntity.kt" \
      "$SRC/com/jarabaimport/levantamiento/data/sample/SampleTank.kt" -name '*.kt') \
      $(find app/src/test -name '*.kt') -d "$OUT/classes" -jvm-target 11 2>&1 | grep -v '^warning' || true
  java -cp "$OUT/classes:$STDLIB" com.jarabaimport.levantamiento.TestRunnerKt
  exit $?
fi

OUT="build/manual/$VARIANT"
rm -rf "$OUT"; mkdir -p "$OUT/classes" "$OUT/dex" dist

echo "==> 1/6 Recursos (aapt2)"
sed "s#<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">#<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" package=\"$APP_ID\">#" \
  app/src/main/AndroidManifest.xml > "$OUT/AndroidManifest.xml"
"$T/aapt2" compile --dir app/src/main/res -o "$OUT/res.zip"
DEBUG_FLAG=""; [ "$VARIANT" = "debug" ] && DEBUG_FLAG="--debug-mode"
VNAME="$VERSION_NAME"; [ "$VARIANT" = "debug" ] && VNAME="$VERSION_NAME-debug"
"$T/aapt2" link -o "$OUT/base.apk" -I "$ANDROID_JAR" --manifest "$OUT/AndroidManifest.xml" \
  --min-sdk-version $MIN_SDK --target-sdk-version $TARGET_SDK \
  --version-code $VERSION_CODE --version-name "$VNAME" $DEBUG_FLAG "$OUT/res.zip"

echo "==> 2/6 Kotlin (kotlinc)"
# Bytecode Java 8 clásico (sin invokedynamic) para máxima compatibilidad con D8 y Android 8+.
"$KOTLINC" $(find "$SRC" -name '*.kt') -no-jdk -cp "$ANDROID_JAR:$STDLIB" -d "$OUT/classes" -jvm-target 1.8 \
  -Xlambdas=class -Xsam-conversions=class -Xstring-concat=inline -no-reflect -nowarn 2>&1 \
  | grep -v '^warning' | tee "$OUT/kotlinc.log"
if grep -q "error:" "$OUT/kotlinc.log"; then echo "ERRORES DE COMPILACIÓN"; exit 1; fi

echo "==> 3/6 Dex (D8)"
MODE="--release"; [ "$VARIANT" = "debug" ] && MODE="--debug"
java -cp "$BUNDLETOOL" com.android.tools.r8.D8 $MODE --min-api $MIN_SDK --lib "$ANDROID_JAR" \
  --output "$OUT/dex" $(find "$OUT/classes" -name '*.class') "$STDLIB" 2>&1 | grep -v "^Info: Unexpected error while reading" || true

[ -f "$OUT/dex/classes.dex" ] || { echo "D8 no generó classes.dex"; exit 1; }

echo "==> 4/6 Empaquetado"
cp "$OUT/base.apk" "$OUT/unsigned.apk"
(cd "$OUT/dex" && zip -q -j ../unsigned.apk classes*.dex)

echo "==> 5/6 Alineación (zipalign)"
"$T/sdk/build-tools/zipalign" -p -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

echo "==> 6/6 Firma (apksig v1+v2+v3)"
if [ "$VARIANT" = "release" ]; then
  [ -f keystore.properties ] || { echo "Falta keystore.properties"; exit 1; }
  prop() { grep "^$1=" keystore.properties | cut -d= -f2-; }
  KS="$(prop storeFile)"; SP="$(prop storePassword)"; KA="$(prop keyAlias)"; KP="$(prop keyPassword)"
else
  KS="keystore/debug.jks"; SP="android"; KA="androiddebugkey"; KP="android"
  [ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -alias "$KA" -storepass "$SP" -keypass "$KP" \
      -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
fi
APK="dist/LevantamientoTanques-$VERSION_NAME-$VARIANT.apk"
CP="$T/signer.jar:$BUNDLETOOL:$STDLIB"
java -cp "$CP" SignerKt "$OUT/aligned.apk" "$APK" "$KS" "$SP" "$KA" "$KP"
java -cp "$CP" VerifyKt "$APK"
ls -la "$APK"
