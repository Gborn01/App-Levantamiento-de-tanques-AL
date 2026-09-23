#!/usr/bin/env python3
"""
Recorrido automático de los 17 criterios de aceptación en un emulador (sin red).

Usa solo adb + uiautomator (sin dependencias en la app). Deja en evidence/:
  - NN_*.png          capturas de cada paso
  - resultados.md     tabla con el resultado de cada criterio
  - export/*.pdf|csv  archivos generados por la app
Sale con código 1 si algún criterio falla.

Uso: python3 tools/e2e/flujo_aceptacion.py [ruta_apk]
"""
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG = "com.jarabaimport.levantamiento"
OUT = "evidence"
os.makedirs(f"{OUT}/export", exist_ok=True)

results = []  # (n, criterio, ok, detalle)
shot_n = [0]


# ------------------------------------------------------------------ adb

def adb(*args, check=False, timeout=60):
    r = subprocess.run(["adb", *args], capture_output=True, timeout=timeout)
    if check and r.returncode != 0:
        raise RuntimeError(f"adb {' '.join(args)}: {r.stderr.decode(errors='replace')}")
    return r.stdout.decode(errors="replace")


def sh(cmd, timeout=60):
    return adb("shell", cmd, timeout=timeout)


def shot(name):
    shot_n[0] += 1
    path = f"{OUT}/{shot_n[0]:02d}_{name}.png"
    with open(path, "wb") as f:
        f.write(subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True).stdout)
    print("  captura:", path)


def screen_size():
    m = re.search(r"(\d+)x(\d+)", sh("wm size"))
    return int(m.group(1)), int(m.group(2))


W, H = 0, 0


# ------------------------------------------------------------------ jerarquía de vistas

class Node:
    def __init__(self, e):
        self.text = e.get("text", "")
        self.desc = e.get("content-desc", "")
        self.cls = e.get("class", "")
        self.rid = e.get("resource-id", "")
        self.pkg = e.get("package", "")
        self.checked = e.get("checked") == "true"
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", e.get("bounds", "[0,0][0,0]")))
        self.bounds = (x1, y1, x2, y2)
        self.cx, self.cy = (x1 + x2) // 2, (y1 + y2) // 2

    def __repr__(self):
        return f"<{self.cls.split('.')[-1]} '{self.text or self.desc}' {self.bounds}>"


def dump(_retry=True):
    for _ in range(5):
        out = sh("uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; cat /sdcard/ui.xml")
        i = out.find("<?xml")
        if i >= 0:
            try:
                root = ET.fromstring(out[i:])
                nodes = [Node(e) for e in root.iter("node")]
            except ET.ParseError:
                nodes = None
            if nodes is not None:
                if _retry and dismiss_system_dialog(nodes):
                    return dump(_retry=False)
                return nodes
        time.sleep(1)
    return []


SYSTEM_DIALOG = ("isn't responding", "keeps stopping", "has stopped", "no responde", "se detuvo")


def dismiss_system_dialog(nodes):
    """Cierra diálogos del sistema de OTRAS apps (p. ej. "Pixel Launcher isn't responding") que tapan la pantalla.
    Un cierre de nuestra app se detecta aparte con el logcat (crashed())."""
    msg = next((n for n in nodes if any(k in n.text for k in SYSTEM_DIALOG)), None)
    if not msg or "Levantamiento" in msg.text:
        return False
    btn = next((n for n in nodes if n.text in ("Wait", "Esperar", "Close app", "Cerrar app", "OK", "Aceptar")), None)
    print("  (diálogo del sistema cerrado:", msg.text, ")")
    if btn:
        sh(f"input tap {btn.cx} {btn.cy}")
    else:
        sh("am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS")
    time.sleep(1.5)
    return True


def norm(s):
    # Botones y títulos de sección se muestran en MAYÚSCULAS: comparar sin distinguir mayúsculas
    return s.replace("\u00a0", " ").strip().casefold()


def find(label, nodes=None, exact=False, cls=None):
    nodes = nodes if nodes is not None else dump()
    for n in nodes:
        if cls and not n.cls.endswith(cls):
            continue
        lab = norm(label)
        for v in (norm(n.text), norm(n.desc)):
            if v and ((v == lab) if exact else (lab in v)):
                return n
    return None


def swipe_up():
    # Con el teclado abierto el gesto caería sobre él (escritura por deslizamiento): cerrarlo antes
    hide_keyboard()
    # Gesto en la mitad superior: nunca cae sobre el teclado en pantalla
    sh(f"input swipe {W // 2} {int(H * 0.55)} {W // 2} {int(H * 0.18)} 400")
    time.sleep(0.8)


def swipe_down():
    hide_keyboard()
    sh(f"input swipe {W // 2} {int(H * 0.18)} {W // 2} {int(H * 0.55)} 400")
    time.sleep(0.8)


def scroll_top():
    for _ in range(6):
        swipe_down()


def seek(label, exact=False, cls=None, max_swipes=14, from_top=False):
    """Busca un texto desplazando hacia abajo."""
    if from_top:
        scroll_top()
    for _ in range(max_swipes + 1):
        n = find(label, exact=exact, cls=cls)
        # Excluir solo la barra de navegación del sistema (la barra del asistente recorta el contenido)
        if n and n.cy < H * 0.94:
            return n
        swipe_up()
    return None


def tap(label, exact=False, cls=None, wait=1.2, **kw):
    n = seek(label, exact=exact, cls=cls, **kw)
    if not n:
        raise AssertionError(f"No se encontró '{label}' en pantalla")
    sh(f"input tap {n.cx} {n.cy}")
    time.sleep(wait)
    return n


def tap_now(label, exact=False, wait=1.2):
    """Toca sin desplazar (barra inferior, diálogos)."""
    n = find(label, exact=exact)
    if not n:
        raise AssertionError(f"No se encontró '{label}' en pantalla")
    sh(f"input tap {n.cx} {n.cy}")
    time.sleep(wait)
    return n


def type_text(s):
    # adb 'input text' no admite espacios literales ni acentos: usar ASCII y %s
    sh("input text " + s.replace(" ", "%s"))
    time.sleep(0.5)


def fill(label, value):
    """Escribe en el EditText que sigue a la etiqueta [label]."""
    for _ in range(15):
        nodes = dump()
        idx = next((i for i, n in enumerate(nodes)
                    if norm(n.text).lstrip("★ ").rstrip(" *") == norm(label) and not n.cls.endswith("EditText")), None)
        if idx is not None:
            et = next((n for n in nodes[idx + 1:] if n.cls.endswith("EditText")), None)
            if et and et.cy < H * 0.94:
                sh(f"input tap {et.cx} {et.cy}")
                time.sleep(0.6)
                sh("input keyevent KEYCODE_MOVE_END")
                # borrar contenido previo
                sh("input keyevent " + " ".join(["KEYCODE_DEL"] * max(len(et.text), 1)))
                type_text(value)
                return
        swipe_up()
    raise AssertionError(f"No se encontró el campo '{label}'")


def hide_keyboard():
    """El teclado en pantalla está desactivado durante la prueba (ver main): no hace falta cerrarlo.
    No se usa ATRÁS para esto: si el teclado no está visible, ATRÁS saldría de la pantalla."""


def has(label, exact=False):
    return find(label, exact=exact) is not None


def wait_for(label, timeout=15, exact=False):
    end = time.time() + timeout
    while time.time() < end:
        if has(label, exact=exact):
            return True
        time.sleep(1)
    return False


def check(n, name, fn):
    print(f"\n== Criterio {n}: {name}")
    try:
        detail = fn() or ""
        results.append((n, name, True, detail))
        print("  OK", detail)
    except Exception as e:  # noqa: BLE001
        results.append((n, name, False, str(e)))
        print("  FALLO:", e)
        print("  En pantalla:", " | ".join(x.text or x.desc for x in dump() if (x.text or x.desc))[:1500])
        shot(f"fallo_criterio_{n}")


def crashed():
    log = adb("logcat", "-d", "-b", "crash")
    return "FATAL EXCEPTION" in log and PKG in log


def launch():
    sh(f"am start -W -n {PKG}/.ui.home.MainActivity")
    time.sleep(2.5)


def go_home():
    sh(f"am start -W -n {PKG}/.ui.home.MainActivity --activity-clear-top")
    time.sleep(2)


def run_as(cmd):
    return sh(f"run-as {PKG} sh -c '{cmd}'")


# ------------------------------------------------------------------ cámara del emulador

def take_camera_photo():
    """Controla la app de cámara del sistema: disparar y confirmar."""
    time.sleep(4)
    for _ in range(4):  # diálogos de primer uso de la cámara
        nodes = dump()
        for lbl in ("NEXT", "Next", "Siguiente", "OK", "Aceptar", "While using the app", "Allow", "ALLOW", "Only this time"):
            n = find(lbl, nodes, exact=True)
            if n and n.pkg != PKG:
                sh(f"input tap {n.cx} {n.cy}")
                time.sleep(1.5)
                break
        else:
            break
    nodes = dump()
    shutter = next((n for n in nodes if n.rid.endswith("shutter_button")), None)
    if shutter:
        sh(f"input tap {shutter.cx} {shutter.cy}")
    else:
        sh("input keyevent KEYCODE_CAMERA")
    time.sleep(4)
    for _ in range(6):
        nodes = dump()
        done = next((n for n in nodes if n.rid.endswith("done_button") or n.desc in ("Done", "Listo", "OK")), None)
        if done:
            sh(f"input tap {done.cx} {done.cy}")
            time.sleep(3)
            return True
        if any(n.pkg == PKG for n in nodes):
            return True  # la cámara devolvió el resultado directamente
        time.sleep(1.5)
    return False


def pick_from_gallery():
    """Elige una imagen previamente copiada a /sdcard/Pictures con el selector de documentos."""
    time.sleep(3)
    for _ in range(3):
        nodes = dump()
        # Selector de apps (createChooser) → elegir "Files"/"Archivos" si aparece
        n = find("Files", nodes, exact=True) or find("Archivos", nodes, exact=True)
        if n and n.pkg != PKG and "documentsui" not in n.pkg:
            sh(f"input tap {n.cx} {n.cy}")
            time.sleep(3)
            continue
        img = next((x for x in nodes if "prueba_tanque" in x.text or "prueba_tanque" in x.desc), None)
        if img:
            sh(f"input tap {img.cx} {img.cy}")
            time.sleep(3)
            return True
        # Navegar a Imágenes / Pictures
        for lbl in ("Images", "Imágenes", "Pictures"):
            m = find(lbl, nodes, exact=True)
            if m:
                sh(f"input tap {m.cx} {m.cy}")
                time.sleep(2)
                break
    return False


# ------------------------------------------------------------------ recorrido

def main():
    global W, H
    apk = sys.argv[1] if len(sys.argv) > 1 else None
    W, H = screen_size()
    print("Pantalla:", W, H)

    # 17. Sin Internet: sin wifi / datos, y la app no declara permiso INTERNET
    sh("svc wifi disable")
    sh("svc data disable")
    sh("cmd connectivity airplane-mode enable")
    sh("settings put global stay_on_while_plugged_in 7")
    sh("input keyevent KEYCODE_WAKEUP")
    sh("wm dismiss-keyguard")
    # El emulador de CI es lento: dejar que el sistema se asiente y no mostrar diálogos de "no responde"
    sh("settings put global hide_error_dialogs 1")
    sh("input keyevent KEYCODE_HOME")
    time.sleep(15)
    dump()
    # Sin teclado en pantalla: 'input text' envía las teclas directamente al campo enfocado
    for ime in sh("ime list -s").split():
        sh(f"ime disable {ime}")
        sh(f"pm disable-user --user 0 {ime.split('/')[0]}")
    print("Teclados activos tras desactivar:", sh("ime list -s").split() or "ninguno")
    if apk:
        print(adb("install", "-r", "-g", apk, timeout=180))
    sh(f"pm clear {PKG}")
    adb("logcat", "-c")

    # Imagen para la galería (alternativa a la cámara)
    subprocess.run([sys.executable, os.path.join(os.path.dirname(__file__), "imagen_prueba.py"), f"{OUT}/prueba_tanque.png"], check=True)
    adb("push", f"{OUT}/prueba_tanque.png", "/sdcard/Pictures/prueba_tanque.png")
    sh("am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures/prueba_tanque.png")
    sh("content call --method scan_volume --uri content://media --arg external_primary")

    def c1():
        launch()
        if crashed():
            raise AssertionError("La app se cerró al abrir:\n" + adb("logcat", "-d", "-b", "crash")[-3000:])
        if not wait_for("LEVANTAMIENTO"):
            raise AssertionError("No se ve la pantalla de inicio")
        shot("inicio")
        return "Pantalla de inicio visible, sin cierres"
    check(1, "Abrir la app", c1)

    def c2():
        tap("+ Nuevo cliente", from_top=True)
        wait_for("Nuevo cliente")
        fill("Nombre del cliente", "Cliente Prueba")
        fill("Contacto", "Ana Lopez")
        fill("Ciudad", "Santiago")
        hide_keyboard()
        shot("formulario_cliente")
        tap("Guardar cliente")
        if not wait_for("Detalle del cliente"):
            raise AssertionError("No se abrió el detalle del cliente tras guardar")
        return "Cliente 'Cliente Prueba' guardado"
    check(2, "Crear cliente", c2)

    def c3():
        if not has("Detalle del cliente") or not has("Cliente Prueba"):
            raise AssertionError("El detalle no muestra el cliente")
        shot("detalle_cliente")
        return "Detalle del cliente abierto"
    check(3, "Entrar al cliente", c3)

    def c5():
        tap("+ Nuevo tanque", from_top=True)
        wait_for("PASO 1 DE 8")
        fill("Nombre del tanque", "Tanque Salsa")
        fill("Área / planta", "Planta 2")
        hide_keyboard()
        shot("paso1_identificacion")
        tap_now("Siguiente →")
        wait_for("PASO 2 DE 8")
        fill("Producto procesado", "Salsa de tomate")
        fill("Residuo después del vaciado", "Pelicula viscosa en paredes")
        hide_keyboard()
        tap("Viscoso", exact=False)
        shot("paso2_producto")
        tap_now("Siguiente →")
        wait_for("PASO 3 DE 8")
        fill("Diámetro interno", "2000")
        fill("Altura cilíndrica", "3000")
        fill("Volumen nominal", "10000")
        hide_keyboard()
        if not seek("9,425 L"):  # π/4 × 2² × 3 m³ ≈ 9,425 L
            raise AssertionError("No se muestra el volumen estimado (≈ 9,425 L)")
        shot("paso3_geometria")
        tap_now("Siguiente →")
        wait_for("PASO 4 DE 8")
        fill("Caudal disponible", "20")
        fill("Presión disponible en el cabezal", "3")
        hide_keyboard()
        shot("paso4_cip")
        tap_now("Siguiente →")
        wait_for("PASO 5 DE 8")
        tap("Agitador", exact=True)
        fill("Diámetro del agitador", "1200")
        hide_keyboard()
        shot("paso5_internos")
        tap_now("Siguiente →")
        wait_for("PASO 6 DE 8")
        tap("No", exact=True)
        shot("paso6_cabezal")
        tap_now("Siguiente →")
        if not wait_for("PASO 7 DE 8"):
            raise AssertionError("No se llegó al paso 7")
        return "Pasos 1–6 completados con los 7 datos principales"
    check(5, "Completar un levantamiento (asistente 8 pasos)", c5)

    def c12():
        wait_for("PASO 7 DE 8")
        shot("paso7_fotos")
        tap("Tomar foto", from_top=True, wait=1)
        how = "cámara"
        ok = take_camera_photo()
        if not wait_for("PASO 7 DE 8", timeout=20):
            sh("input keyevent KEYCODE_BACK")
            wait_for("PASO 7 DE 8", timeout=10)
            ok = False
        time.sleep(3)
        if not ok or not has("(1)"):
            how = "galería"
            tap("Galería", from_top=True, wait=1)
            if not pick_from_gallery():
                raise AssertionError("No se pudo tomar ni elegir una foto")
            wait_for("PASO 7 DE 8", timeout=20)
            time.sleep(3)
        shot("paso7_foto_tomada")
        n = run_as("ls files/photos/*/ | wc -l").strip()
        if not has("(1)") and not has("(2)"):
            raise AssertionError(f"La foto no aparece en la categoría (archivos en disco: {n})")
        return f"Foto añadida con {how}; archivos en disco: {n}"
    check(12, "Tomar fotografías", c12)

    def c6():
        tap_now("Siguiente →")
        wait_for("PASO 8 DE 8")
        fill("Técnico responsable", "Juan Perez")
        fill("Observaciones generales", "Levantamiento de prueba automatica")
        hide_keyboard()
        if not seek("Datos principales: 7 / 7", from_top=True):
            raise AssertionError("El resumen del paso 8 no marca 7 / 7")
        shot("paso8_observaciones")
        tap_now("✓ Finalizar", wait=2)
        if not wait_for("Resumen del levantamiento"):
            raise AssertionError("No se abrió el resumen tras finalizar")
        if not has("7 / 7 datos principales"):
            raise AssertionError("El resumen no muestra 7 / 7 datos")
        shot("resumen_tanque")
        return "Guardado; resumen con 7 / 7 datos y estado completado"
    check(6, "Guardar el levantamiento", c6)

    def c13():
        if not seek("Fotografías (1)") and not seek("Fotografías (2)", from_top=True):
            raise AssertionError("El resumen no muestra la foto")
        shot("resumen_fotos")
        return "La foto aparece en el resumen del tanque"
    check(13, "Ver las fotos en el tanque", c13)

    def c4():
        sh("input keyevent KEYCODE_BACK")  # resumen → detalle del cliente
        time.sleep(1.5)
        if not has("Detalle del cliente"):
            raise AssertionError("No se volvió al detalle del cliente")
        tap("+ Nuevo tanque", from_top=True)
        wait_for("PASO 1 DE 8")
        fill("Nombre del tanque", "Tanque Mayonesa")
        hide_keyboard()
        tap_now("Siguiente →")
        wait_for("PASO 2 DE 8")
        fill("Producto procesado", "Mayonesa")
        hide_keyboard()
        time.sleep(1.5)  # autoguardado
        sh("input keyevent KEYCODE_BACK")  # salir: se guarda como borrador
        time.sleep(2)
        if not wait_for("Tanques (2)"):
            raise AssertionError("El cliente no muestra 2 tanques")
        shot("cliente_dos_tanques")
        return "Cliente con 2 tanques (uno completo y uno borrador)"
    check(4, "Crear varios tanques", c4)

    def c11():
        tap("Tanque Salsa", from_top=True, wait=2)
        wait_for("Resumen del levantamiento")
        tap("Duplicar tanque")
        tap_now("Duplicar", exact=True, wait=2.5)
        if not wait_for("PASO 1 DE 8"):
            raise AssertionError("No se abrió el asistente con la copia")
        shot("duplicado_asistente")
        sh("input keyevent KEYCODE_BACK")
        time.sleep(2)
        sh("input keyevent KEYCODE_BACK")
        time.sleep(1.5)
        if not has("Detalle del cliente"):
            go_home(); tap("Clientes", exact=True); tap("Cliente Prueba")
        if not wait_for("Tanques (3)"):
            raise AssertionError("Tras duplicar el cliente no tiene 3 tanques")
        if not has("TK-003"):
            raise AssertionError("La copia no recibió el siguiente código (TK-003)")
        shot("cliente_tres_tanques")
        return "Copia creada con código TK-003"
    check(11, "Duplicar tanque", c11)

    def c10():
        tap("Tanque Salsa", from_top=True, wait=2)
        wait_for("Resumen del levantamiento")
        tap("Editar levantamiento")
        wait_for("PASO 1 DE 8")
        tap_now("Siguiente →")
        wait_for("PASO 2 DE 8")
        fill("Producto procesado", "Salsa picante")
        hide_keyboard()
        time.sleep(1.5)
        sh("input keyevent KEYCODE_BACK")
        time.sleep(2)
        if not wait_for("Salsa picante"):
            raise AssertionError("El resumen no refleja el producto editado")
        shot("tanque_editado")
        return "Producto cambiado a 'Salsa picante' y visible en el resumen"
    check(10, "Editar tanque", c10)

    def c7():
        sh(f"am force-stop {PKG}")
        time.sleep(1.5)
        if sh(f"pidof {PKG}").strip():
            raise AssertionError("El proceso sigue vivo")
        return "Proceso detenido (force-stop)"
    check(7, "Cerrar la app", c7)

    def c8():
        launch()
        if crashed():
            raise AssertionError("La app se cerró al reabrir")
        if not wait_for("LEVANTAMIENTO"):
            raise AssertionError("No se ve la pantalla de inicio al reabrir")
        shot("reabierta")
        return "Reabierta correctamente"
    check(8, "Reabrir la app", c8)

    def c9():
        # Inicio: 2 clientes (ejemplo + prueba), 4 tanques
        tap("Clientes", exact=True)
        wait_for("Cliente Prueba")
        shot("lista_clientes")
        tap("Cliente Prueba", wait=2)
        if not wait_for("Tanques (3)"):
            raise AssertionError("Tras reabrir, el cliente no tiene sus 3 tanques")
        tap("Tanque Salsa", from_top=True, wait=2)
        for v in ("Salsa picante", "2,000 mm", "3 bar", "20 m³/h", "Agitador"):
            if not seek(v, from_top=True):
                raise AssertionError(f"Falta el dato guardado '{v}'")
        shot("datos_guardados")
        return "Cliente, 3 tanques y datos del levantamiento persisten tras reiniciar"
    check(9, "Ver los datos guardados", c9)

    def export(kind):
        run_as("rm -rf cache/exports")
        tap(f"Exportar {kind}", from_top=True, wait=1)
        if not wait_for("Archivo listo", timeout=40):
            raise AssertionError(f"No apareció 'Archivo listo' al exportar {kind}")
        shot(f"export_{kind.lower()}")
        files = run_as("ls cache/exports").split()
        f = next((x for x in files if x.lower().endswith("." + kind.lower())), None)
        if not f:
            raise AssertionError(f"No se generó el archivo {kind}: {files}")
        data = subprocess.run(["adb", "exec-out", "run-as", PKG, "cat", f"cache/exports/{f}"], capture_output=True).stdout
        with open(f"{OUT}/export/{f}", "wb") as fh:
            fh.write(data)
        sh("input keyevent KEYCODE_BACK")
        time.sleep(1)
        return f, data

    def c15():
        f, data = export("PDF")
        if not data.startswith(b"%PDF") or len(data) < 2000:
            raise AssertionError(f"PDF inválido ({len(data)} bytes)")
        return f"{f} ({len(data) // 1024} KB)"
    check(15, "Generar PDF", c15)

    def c16():
        f, data = export("CSV")
        text = data.decode("utf-8", errors="replace")
        if "Tanque Salsa" not in text or "Salsa picante" not in text:
            raise AssertionError("El CSV no contiene los datos del tanque")
        return f"{f} ({len(text.splitlines())} líneas)"
    check(16, "Exportar CSV", c16)

    def c14():
        go_home()
        tap("Clientes", exact=True)
        n = next(x for x in dump() if x.cls.endswith("EditText"))
        sh(f"input tap {n.cx} {n.cy}")
        time.sleep(0.5)
        type_text("Mayonesa")
        hide_keyboard()
        # "Tanque de mayonesa" (datos de ejemplo) también coincide; basta con que aparezca el nuestro
        if not wait_for("Tanque Mayonesa"):
            raise AssertionError("La búsqueda por producto no encontró el tanque")
        shot("busqueda_tanque")
        sh("input keyevent " + " ".join(["KEYCODE_DEL"] * 10))
        n = next(x for x in dump() if x.cls.endswith("EditText"))
        sh(f"input tap {n.cx} {n.cy}")
        sh("input keyevent KEYCODE_MOVE_END")
        sh("input keyevent " + " ".join(["KEYCODE_DEL"] * 10))
        type_text("Prueba")
        hide_keyboard()
        if not wait_for("Clientes (1)") or not has("Cliente Prueba"):
            raise AssertionError("La búsqueda de clientes no encontró 'Cliente Prueba'")
        shot("busqueda_cliente")
        return "Búsqueda por producto y por nombre de cliente"
    check(14, "Buscar clientes y tanques", c14)

    def c17():
        perms = sh(f"dumpsys package {PKG} | grep -i 'android.permission.INTERNET'")
        if perms.strip():
            raise AssertionError("La app declara el permiso INTERNET")
        net = sh("settings get global airplane_mode_on").strip()
        return f"Sin permiso INTERNET; modo avión={net}; wifi y datos desactivados durante toda la prueba"
    check(17, "Todo sin Internet", c17)

    if crashed():
        results.append((0, "Sin cierres durante la prueba", False, adb("logcat", "-d", "-b", "crash")[-3000:]))

    adb("logcat", "-d", "-f", "/dev/null")
    with open(f"{OUT}/logcat.txt", "w") as fh:
        fh.write(adb("logcat", "-d", timeout=120))
    with open(f"{OUT}/crash.txt", "w") as fh:
        fh.write(adb("logcat", "-d", "-b", "crash"))

    results.sort(key=lambda r: r[0])
    ok = all(r[2] for r in results)
    with open(f"{OUT}/resultados.md", "w") as fh:
        fh.write("# Criterios de aceptación (emulador, sin red)\n\n| # | Criterio | Resultado | Detalle |\n|---|---|---|---|\n")
        for n, name, passed, detail in results:
            d = detail.replace("\n", " ").replace("|", "/")[:300]
            fh.write(f"| {n} | {name} | {'✅' if passed else '❌'} | {d} |\n")
    print(open(f"{OUT}/resultados.md").read())
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
