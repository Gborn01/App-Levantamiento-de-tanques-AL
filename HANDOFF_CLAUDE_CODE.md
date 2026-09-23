# Traspaso a Claude Code – App "Levantamiento de Tanques" (Android)

> **Para Claude Code:** este documento contiene todo el contexto. El proyecto completo está en este repositorio.
> La especificación original del cliente está en `docs/ESPECIFICACION_ORIGINAL.md` (léela completa: es el requisito).
> Trabaja en español con el usuario.

---

## 1. Objetivo inmediato

1. Subir este proyecto a un repositorio **privado** de GitHub del usuario.
2. Compilar con **Gradle** (GitHub Actions ya configurado en `.github/workflows/android.yml`).
3. **Corregir el cierre al arrancar** (ver §3) usando el logcat del emulador que produce el flujo de CI.
4. Verificar en emulador los 17 criterios de aceptación (§6) y entregar al usuario la **APK release** funcional.

## 2. Estado actual

- Código **completo** para V1: clientes, tanques, asistente de 8 pasos con autoguardado, 7 datos principales,
  validación, cálculos (volumen estimado, H/D), fotos (cámara/galería, 11 categorías), firma, resumen,
  duplicar, búsqueda, estados, PDF y CSV, datos de ejemplo (Bepensa TK-001).
- Stack: **Kotlin + vistas nativas de Android + SQLite nativo (SQLiteOpenHelper)**. **Sin dependencias externas**
  (sin AndroidX, sin Compose, sin Room). Se eligió así porque el entorno anterior no tenía acceso a Google Maven.
  Si el usuario lo pide más adelante, se puede migrar a Compose + Room (el paquete `domain/` es Kotlin puro y se reutiliza).
- `minSdk 26`, `targetSdk 34`, `compileSdk 35`, Kotlin 2.0.21, AGP 8.7.3, Gradle wrapper 8.11.1.
- **La configuración Gradle nunca se ha ejecutado** (el entorno anterior no tenía red para Gradle). Puede requerir ajustes menores.
- Las APK anteriores se generaron **sin Gradle**, con un script manual (`build.sh`: aapt2 + kotlinc + un D8 antiguo 3.3.28
  + apksig). Compilan y se firman, pero **la app se cierra al abrir** en el teléfono del usuario.
- Pruebas de lógica: `./build.sh test` (58 comprobaciones, JVM, sin JUnit) — pasan. Opcional: convertirlas a JUnit
  (`app/src/test/.../TestRunner.kt`), actualmente excluidas de Gradle en `app/build.gradle.kts` (`sourceSets["test"]`).

## 3. Problema abierto: se cierra al arrancar

- Dispositivo del usuario: **Xiaomi (MIUI), Android 11–15**. Mensaje: "Levantamiento Tanques continúa deteniéndose".
- No hay stack trace todavía. Nunca se ejecutó en emulador.
- Hipótesis (por orden de probabilidad):
  1. **Toolchain manual** (D8 3.3.28 antiguo frente a Kotlin 2.0, aapt2 de terceros, empaquetado manual). Compilar con
     Gradle/AGP puede resolverlo por sí solo.
  2. Error de ejecución en el arranque: `LevantamientoApp.onCreate` (crea BD SQLite y carga datos de ejemplo),
     `AppFileProvider` (ContentProvider propio), `ui/home/MainActivity` + `ui/BaseActivity` (construcción de vistas
     por código), tema `res/values/themes.xml`, icono adaptativo `res/drawable/ic_launcher_foreground.xml`.
- El flujo de CI instala la APK debug en un emulador API 33 **sin red**, la abre y **falla mostrando el `FATAL EXCEPTION`**
  del logcat (artefacto `evidence/`: `logcat.txt`, `crash.txt`, `inicio.png`). Usar ese stack trace para corregir.
- La app incluye `util/CrashReporter.kt` + `util/CrashReportActivity.kt`: guarda el último error y lo muestra al reabrir.

## 4. Cómo compilar

```bash
./gradlew assembleDebug      # app/build/outputs/apk/debug/
./gradlew assembleRelease    # app/build/outputs/apk/release/ (firmada con keystore.properties)
```
- Firma release: `keystore/levantamiento-release.jks` + `keystore.properties` (incluidos; repo **privado**).
  **No regenerar la clave**: el usuario necesita la misma para futuras actualizaciones.
- `build.sh` + `tools/` = compilación manual alternativa (puede ignorarse o eliminarse si Gradle funciona).

## 5. Arquitectura (resumen)

```
app/src/main/java/com/jarabaimport/levantamiento/
  LevantamientoApp.kt, AppContainer.kt   arranque + DI manual
  data/db/        DbHelper (esquema v1), Daos.kt, entidades, TankMapping.kt (72 columnas del tanque)
  data/repository Repositorios (clientes, tanques, fotos, preferencias, archivos)
  data/sample/    Datos de ejemplo (Bepensa TK-001)
  domain/         Lógica pura: TankCalculations, TankValidator, KeyData (7 datos + estado), TankDuplicator,
                  model/Enums.kt, selection/ (contrato para futura selección de cabezal; NO recomienda nada en V1)
  export/         PdfReportGenerator (android.graphics.pdf), CsvExporter, ExportManager, BrandingConfig
  util/           ImageUtils (EXIF/reducción), AppFileProvider (FileProvider propio), CrashReporter
  ui/             Ui.kt (kit visual), BaseActivity, home/, clients/, tank/ (asistente, resumen, firma), photos/
```
- Relaciones: Cliente 1→N Tanques, Tanque 1→N Fotos, con `ON DELETE CASCADE`. IDs + UUID.
- Unidades internas normalizadas: mm, L, °, m³/h, bar, °C, %, min, pulgadas, m.
- Estética pedida por el usuario: **fondo claro**, línea visual inspirada en Alfa Laval (azul corporativo profundo,
  blanco, grises). Colores en `ui/Ui.kt → Palette` y `export/BrandingConfig.kt`. **No usar logo de Alfa Laval**
  (hay espacio para `res/drawable/brand_logo.png` autorizado).
- Sin permiso de INTERNET (100 % offline por diseño).

## 6. Criterios de aceptación a verificar (en emulador, sin red)

1. Abrir la app · 2. Crear cliente · 3. Entrar al cliente · 4. Crear varios tanques · 5. Completar un levantamiento ·
6. Guardarlo · 7. Cerrar la app · 8. Reabrirla · 9. Ver los datos guardados · 10. Editar tanque · 11. Duplicar tanque ·
12. Tomar fotografías · 13. Verlas en el tanque · 14. Buscar clientes y tanques · 15. Generar PDF · 16. Exportar CSV ·
17. Todo sin Internet.

Recomendado: pruebas instrumentadas (Espresso/UiAutomator) o un script `adb` que recorra el flujo, con capturas
de pantalla que se suban como artefactos para que el usuario las vea.

## 7. Entregables al usuario

- APK release funcional (firmada con la clave incluida) y APK debug.
- Repositorio con el código y README actualizado (quitar la nota de "no probado en emulador" cuando se verifique).
- Capturas de pantalla de la app funcionando.
- Lista de mejoras V2 (ya en README §12) — **no implementarlas** sin que el usuario lo pida.

## 8. Reglas del usuario (de la especificación)

- Priorizar: APK funcional > offline > BD local > mantenible > fácil de ampliar. Nada de funciones innecesarias.
- No hacer diagnósticos automáticos del producto ni recomendaciones de modelos Alfa Laval en V1.
- Producto y residuo son campos distintos.
- Interfaz: botones grandes, pocos elementos por pantalla, teclado numérico en medidas, desplegables, autoguardado,
  sin animaciones innecesarias.
