# Levantamiento de Tanques – V1

> **Estado:** las APK compiladas sin Gradle se cierran al abrir en el teléfono de prueba. Ver `HANDOFF_CLAUDE_CODE.md`
> para compilar con Gradle/GitHub Actions y corregirlo.

App Android **100 % offline** para registrar en campo el levantamiento técnico de tanques de producción
(clientes → tanques → levantamiento), como base para la posterior selección de cabezales de limpieza.

- Kotlin + vistas nativas de Android + SQLite nativo. **Sin librerías externas.**
- Sin servidor, sin Firebase y **sin permiso de Internet**: el sistema operativo no le deja a la app conectarse a la red.
- Datos, fotos y firmas se guardan en el almacenamiento interno del teléfono.

---

## 1. Entregables

| Archivo | Uso |
|---|---|
| `dist/LevantamientoTanques-1.0.0-release.apk` | **APK para instalar en los teléfonos** (firmada con la clave de release). |
| `dist/LevantamientoTanques-1.0.0-debug.apk` | APK de pruebas (firma de depuración). |
| `keystore/levantamiento-release.jks` + `keystore.properties` | Clave de firma de release. **Guárdelas en lugar seguro** (ver §4). |
| `app/src/...` | Código fuente completo. |
| `build.sh` | Compilación por línea de comandos (sin Android Studio). |
| `tools/setup-toolchain.sh` | Descarga las herramientas de compilación para `build.sh`. |

---

## 2. Instalar la APK en el teléfono

1. Copie `LevantamientoTanques-1.0.0-release.apk` al teléfono (WhatsApp, correo, cable USB o Google Drive).
2. Ábrala desde el teléfono. Si Android lo pide, permita **"Instalar apps de origen desconocido"** para esa app (Archivos, Chrome, WhatsApp…).
3. Pulse **Instalar**. Requiere **Android 8.0 o superior**.
4. Para actualizar a una versión nueva, instale la nueva APK encima: se conservan los datos (siempre que se firme con la misma clave).

> Play Protect puede mostrar un aviso por ser una app que no viene de Google Play; es normal en apps internas. Elija "Instalar de todas formas".

Con cable USB y `adb`: `adb install -r dist/LevantamientoTanques-1.0.0-release.apk`

---

## 3. Compilar

### Opción A – Android Studio (recomendada para seguir desarrollando)
1. Instale Android Studio (Ladybug o posterior) con el SDK de Android 35.
2. *File → Open* → carpeta del proyecto. Android Studio descarga Gradle y el plugin de Android.
3. Compilar:
   - Debug: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/`
   - Release firmada: `./gradlew assembleRelease` → `app/build/outputs/apk/release/` (usa `keystore.properties`)

> Nota: las APK entregadas se compilaron con la Opción B. La configuración Gradle es mínima (sin dependencias) y está
> incluida para trabajar en Android Studio, pero no se pudo ejecutar en el entorno donde se generó el proyecto.

### Opción B – Línea de comandos, sin Android Studio (Linux o WSL en Windows)
Requiere Java 17+, `curl`, `unzip`, `zip`.
```bash
./tools/setup-toolchain.sh      # una sola vez: descarga Kotlin, D8, aapt2, zipalign y android.jar (~200 MB)
./build.sh test                 # pruebas de lógica (58 comprobaciones)
./build.sh debug                # dist/LevantamientoTanques-1.0.0-debug.apk
./build.sh release              # dist/LevantamientoTanques-1.0.0-release.apk
```
El script hace: `aapt2` (recursos) → `kotlinc` (Kotlin) → `D8` (dex) → `zipalign` → firma v2+v3 (`apksig`) → verificación.

Para cambiar la versión: `VERSION_CODE` / `VERSION_NAME` en `build.sh` y en `app/build.gradle.kts`.

---

## 4. Firma de release (importante)

- `keystore/levantamiento-release.jks` y `keystore.properties` (contraseñas) se generaron para este proyecto.
- **Sin ese archivo no podrá publicar actualizaciones** que se instalen encima de la versión actual (habría que desinstalar y se perderían los datos del teléfono).
- Haga una copia en un lugar seguro y no lo comparta públicamente.

---

## 5. Uso rápido

**Inicio** → `+ NUEVO CLIENTE` · `CLIENTES` · `LEVANTAMIENTOS RECIENTES` · `EXPORTAR TODO (CSV)` + estadísticas.

**Cliente** → `+ NUEVO TANQUE` · `EDITAR CLIENTE` · `EXPORTAR` (PDF o CSV de todos sus tanques) · lista de tanques.

**Nuevo tanque** (asistente de 8 pasos, con autoguardado):
1. Identificación · 2. Producto y residuo · 3. Geometría (+ volumen estimado y H/D) · 4. CIP · 5. Internos ·
6. Cabezal · 7. Fotografías (11 categorías, cámara o galería) · 8. Observaciones y firma.

Los números en la parte superior permiten saltar entre pasos. `← Anterior` / `Siguiente →` / `✓ Finalizar`.

**Resumen del levantamiento** → los datos clave en grande (★ = uno de los 7 datos principales) y botones
`EDITAR LEVANTAMIENTO`, `DUPLICAR TANQUE`, `EXPORTAR PDF`, `EXPORTAR CSV`, `CAMBIAR ESTADO`, fotos.

**Exportar**: al generar el archivo se ofrece *Compartir* (WhatsApp, correo, Drive…), *Guardar en el teléfono* (elige carpeta, p. ej. Descargas) o *Abrir*.

### Reglas de funcionamiento
- **Los 7 datos principales**: producto, residuo, diámetro interno, altura, presión CIP, caudal CIP y obstáculos internos
  (marcar "Sin obstáculos internos" cuenta como dato). Se destacan con ★ y borde amarillo.
- **Producto ≠ residuo**: son campos separados; el residuo es texto libre + características seleccionables.
- **Estado**: *Borrador* por defecto; pasa a *Levantamiento completado* automáticamente al tener los 7 datos.
  *Pendiente de revisión* y *Revisado* se asignan a mano con "Cambiar estado".
- **Validación**: nombre del tanque y producto obligatorios; diámetro y alturas > 0; caudal, presión y volúmenes ≥ 0;
  volumen de trabajo ≤ nominal; altura total ≥ cilíndrica; % entre 0 y 100; temperatura 0–150 °C; ángulo 0–90°.
  No se puede avanzar de paso ni finalizar con errores; los mensajes aparecen en rojo bajo cada campo.
- **Autoguardado**: cada cambio se guarda en menos de 1 s, al cambiar de paso y al salir de la app.
  Los números inválidos nunca se guardan. Si la app se cierra inesperadamente, al volver aparece
  *"Hay un levantamiento sin terminar. ¿Desea continuar?"*.
- **Duplicar**: copia todos los datos técnicos, propone el siguiente código (TK-001 → TK-002) y abre el asistente;
  no copia fotos ni firma.
- **Búsqueda** (pantalla Clientes): por nombre de cliente, código de tanque, nombre de tanque o producto.
- **Presión CIP**: se pide la presión *medida en la entrada del cabezal durante el CIP* y se registra el origen del dato.
- **Unidades internas**: mm, L, grados, m³/h, bar, °C, %, min, pulgadas, m. El volumen se puede introducir en L o m³.
- **No hay recomendaciones automáticas** de modelos de cabezal (ver §9).

---

## 6. Datos de prueba
En la primera ejecución se crea automáticamente (puede borrarse desde la app):
- Cliente **Bepensa** (Santo Domingo)
- Tanque **TK-001 – Tanque de mayonesa**, proceso *Preparación*, producto *Mayonesa*,
  residuo *"Película grasa y viscosa adherida a paredes y agitador después del vaciado."*,
  Ø 1,500 mm, altura 2,000 mm, 3,500 L, presión CIP 5.5 bar, caudal 8 m³/h, agitador Ø 1,000 mm, serpentín sí.
  → estado *Levantamiento completado* (7/7). Volumen estimado 3,534 L; H/D 1.33.

Código: `app/src/main/java/.../data/sample/SampleTank.kt` y `SampleData.kt`.

---

## 7. Estructura de carpetas

```
LevantamientoTanques/
├── build.sh                      Compilación sin Android Studio
├── tools/setup-toolchain.sh      Descarga de herramientas para build.sh
├── tools/signer/                 Firmador/verificador de APK (apksig)
├── keystore/ + keystore.properties   Clave de release (¡guardar!)
├── build.gradle.kts, settings.gradle.kts, gradlew…   Proyecto Gradle / Android Studio
└── app/src/
    ├── main/AndroidManifest.xml  Pantallas, proveedor de archivos, SIN permiso de Internet
    ├── main/res/                 Icono, tema claro, textos
    ├── main/java/com/jarabaimport/levantamiento/
    │   ├── LevantamientoApp.kt   Arranque (carga datos de prueba la 1.ª vez)
    │   ├── AppContainer.kt       Inyección de dependencias manual
    │   ├── data/
    │   │   ├── db/               SQLite: DbHelper (esquema), Daos, entidades, TankMapping
    │   │   ├── repository/       Repositorios (clientes, tanques, fotos, preferencias, archivos)
    │   │   └── sample/           Datos de prueba
    │   ├── domain/               Lógica de negocio pura (sin Android)
    │   │   ├── model/Enums.kt    Estados, características de residuo, categorías de foto, opciones
    │   │   ├── TankCalculations  Volumen estimado, H/D
    │   │   ├── TankValidator     Validaciones + saneado para autoguardado
    │   │   ├── KeyData           Los 7 datos principales y estado automático
    │   │   ├── TankDuplicator    Duplicar y siguiente código
    │   │   └── selection/        Contrato para el FUTURO módulo de selección (sin implementar)
    │   ├── export/               PdfReportGenerator, CsvExporter, ExportManager, BrandingConfig
    │   ├── util/                 ImageUtils (rotación/reducción de fotos), AppFileProvider
    │   └── ui/                   Pantallas
    │       ├── Ui.kt             Kit visual (colores, botones grandes, campos, tarjetas)
    │       ├── BaseActivity.kt   Estructura común, diálogos, exportación, tareas en segundo plano
    │       ├── home/             Inicio, recientes
    │       ├── clients/          Lista + búsqueda, detalle, formulario
    │       ├── tank/             Asistente (8 pasos), resumen, panel de firma
    │       └── photos/           Galería por categorías y visor
    └── test/…/TestRunner.kt      Pruebas de lógica (./build.sh test)
```

---

## 8. Arquitectura

Capas con dependencias en un solo sentido: **UI → Repositorios → Base de datos**, y todas usan **Dominio**.

- **Dominio** (`domain/`): Kotlin puro, sin Android. Cálculos, validación, 7 datos principales, estados,
  duplicado. Es lo que se prueba con `./build.sh test` y lo que usará el futuro módulo de selección.
- **Datos** (`data/`): SQLite nativo con claves foráneas y borrado en cascada
  (Cliente 1→N Tanques, Tanque 1→N Fotografías). IDs numéricos + UUID único por registro.
  Para cambiar el esquema: subir `DbHelper.VERSION` y añadir la migración en `onUpgrade` (nunca borrar tablas).
- **Exportación** (`export/`): PDF con la API nativa `PdfDocument` (A4, portada, 7 datos principales, todas las
  secciones, firma y fotos); CSV UTF-8 con BOM, columnas estables `nombre_unidad` y `schema_version`
  (columnas nuevas siempre al final).
- **UI** (`ui/`): una Activity por pantalla; kit visual propio (`Ui.kt`) con botones ≥ 60 dp, campos grandes,
  teclado numérico en medidas, desplegables (▼) para opciones repetitivas y sin animaciones.
- **Estética**: fondo claro, azul corporativo profundo, grises suaves y línea de acento, inspirada en la línea
  visual de Alfa Laval. Los colores están centralizados en `ui/Ui.kt → Palette` (y `export/BrandingConfig.kt`
  para el PDF) para ajustarlos a los valores exactos del manual de marca autorizado.

### Branding / logos
- El PDF muestra "Jaraba Import" como texto. **No se incluye ningún logo de Alfa Laval.**
- Para añadir logos **autorizados** sin tocar código: copie
  `app/src/main/res/drawable/brand_logo.png` (logo principal) y/o `partner_logo.png` (socio) y recompile.

---

## 9. Preparado para la fase "Selección de cabezal"
`domain/selection/CleaningHeadSelection.kt` define:
- `SelectionInput.from(tanque)`: los datos normalizados (7 datos + geometría, H/D, obstáculos, conexión, posición).
- `HeadCandidate` / `SelectionResult`: familia, modelo, boquilla, presión y caudal requeridos, cobertura,
  compatibilidad con geometría y obstáculos, notas técnicas.
- `CleaningHeadSelector`: interfaz a implementar (reglas y/o base de datos de productos).
  En V1 la implementación es `NoSelectionYet` y **no recomienda nada**.
El CSV (`schema_version = 1`) ya contiene esas columnas para alimentar una herramienta externa.

---

## 10. Verificación realizada en V1
- ✔ Compilación completa sin errores (Kotlin 2.0.21, API 35) y APK debug + release firmadas y verificadas (v2+v3).
- ✔ Manifiesto: minSdk 26, targetSdk 34, **sin ningún permiso** (ni Internet ni almacenamiento).
- ✔ Esquema SQLite, mapeo de las 72 columnas del tanque, consultas de lista/búsqueda/recientes y borrado en cascada
  ejecutados contra SQLite real.
- ✔ 58 pruebas de lógica: cálculos, validaciones, autoguardado seguro, 7 datos y estados, duplicado, CSV.
- ✖ **No se pudo ejecutar en un emulador** en el entorno de desarrollo. La prueba en pantalla debe hacerse en un
  teléfono siguiendo la lista del §11.

## 11. Lista de aceptación (probar en el teléfono)
1. Abrir la app · 2. Crear un cliente · 3. Entrar al cliente · 4. Crear varios tanques · 5. Completar un levantamiento ·
6. Guardarlo · 7. Cerrar la app (quitarla de recientes) · 8. Volver a abrirla · 9. Encontrar los datos ·
10. Editar un tanque · 11. Duplicar un tanque · 12. Tomar fotografías · 13. Verlas en el tanque ·
14. Buscar clientes y tanques · 15. Generar PDF · 16. Exportar CSV · 17. Todo en modo avión.

---

## 12. Posibles mejoras para V2 (no implementadas)
1. Módulo de **selección de cabezal** (reglas técnicas / base de datos de productos) usando `SelectionInput`.
2. **Copia de seguridad / restauración** completa (base de datos + fotos en un .zip) y traspaso entre teléfonos.
3. Importar clientes/tanques desde CSV o Excel.
4. Sincronización opcional con la nube (Drive/OneDrive) cuando haya Internet, manteniendo el modo offline.
5. Esquema acotado del tanque en el PDF (dibujo con cotas: diámetro, alturas, posición del cabezal y agitador).
6. Anotar sobre las fotos (flechas/texto) y pie de foto editable.
7. Varias observaciones con fecha por tanque (historial de visitas).
8. Plantillas de tanque por industria (bebidas, salsas, lácteos) para rellenar más rápido.
9. Firma también del representante del cliente y envío del PDF por correo en un paso.
10. Versión iOS (p. ej. con Kotlin Multiplatform reutilizando `domain/`).
11. Modo oscuro opcional y tamaño de letra configurable.
12. Migración a Jetpack Compose + Room si se desea el stack moderno de Google (la lógica de dominio se reutiliza tal cual).
