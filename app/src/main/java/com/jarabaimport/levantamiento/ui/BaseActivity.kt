package com.jarabaimport.levantamiento.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.jarabaimport.levantamiento.AppContainer
import com.jarabaimport.levantamiento.app
import com.jarabaimport.levantamiento.export.ExportManager
import com.jarabaimport.levantamiento.export.ExportedFile
import java.util.concurrent.Executors

/**
 * Base de todas las pantallas: barra superior blanca con título azul y línea de acento,
 * contenido desplazable, barra inferior opcional, tareas en segundo plano y diálogos comunes.
 */
abstract class BaseActivity : Activity() {

    val c: AppContainer get() = app

    lateinit var root: LinearLayout
        private set
    lateinit var scroll: ScrollView
        private set
    /** Contenedor principal (dentro del ScrollView). */
    lateinit var content: LinearLayout
        private set
    private lateinit var titleView: TextView
    private lateinit var subtitleView: TextView
    private lateinit var actionsBox: LinearLayout
    lateinit var bottomBar: LinearLayout
        private set

    private val main = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        styleSystemBars()
    }

    private fun styleSystemBars() {
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }

    /** Construye la estructura de pantalla. Llamar en onCreate. */
    fun setupScreen(title: String, subtitle: String? = null, showBack: Boolean = true, withHeader: Boolean = true) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        if (withHeader) {
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(Color.WHITE)
                setPadding(dp(4), dp(6), dp(8), dp(6))
                minimumHeight = dp(64)
            }
            if (showBack) {
                header.addView(TextView(this).apply {
                    text = "←"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
                    setTextColor(Palette.NAVY)
                    gravity = Gravity.CENTER
                    contentDescription = "Atrás"
                    background = ripple(rounded(Color.TRANSPARENT, dpf(28f)), dpf(28f))
                    setOnClickListener { onBackPressed() }
                }, LinearLayout.LayoutParams(dp(60), dp(60)))
            } else {
                header.addView(View(this), LinearLayout.LayoutParams(dp(12), 1))
            }
            val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            titleView = makeText(this, title, 21f, bold = true, color = Palette.NAVY).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }
            subtitleView = makeText(this, subtitle ?: "", 14f, color = Palette.MUTED).apply {
                maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
                visibility = if (subtitle.isNullOrBlank()) View.GONE else View.VISIBLE
            }
            titles.addView(titleView)
            titles.addView(subtitleView)
            header.addView(titles, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            actionsBox = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            header.addView(actionsBox)
            root.addView(header, lpMatchWrap)
            root.addView(View(this).apply { setBackgroundColor(Palette.ACCENT) },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)))
        } else {
            actionsBox = LinearLayout(this)
            titleView = TextView(this)
            subtitleView = TextView(this)
        }
        scroll = ScrollView(this).apply { isFillViewport = true }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(32))
        }
        scroll.addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            elevation = dpf(8f)
            visibility = View.GONE
        }
        root.addView(bottomBar, lpMatchWrap)
        setContentView(root)
    }

    fun setTitles(title: String, subtitle: String? = null) {
        titleView.text = title
        subtitleView.text = subtitle ?: ""
        subtitleView.visibility = if (subtitle.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    /** Acción de texto en la barra superior (p.ej. "BORRAR"). */
    fun addHeaderAction(label: String, onClick: () -> Unit) {
        actionsBox.addView(makeText(this, label, 14f, bold = true, color = Palette.NAVY).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), 0, dp(12), 0)
            minHeight = dp(48)
            background = ripple(rounded(Color.WHITE, dpf(8f), Palette.NAVY, dp(1)), dpf(8f))
            setOnClickListener { onClick() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)))
    }

    /** Vacía el contenido y lo vuelve a construir (conservando la posición de scroll). */
    fun rebuild(keepScroll: Boolean = true, body: LinearLayout.() -> Unit) {
        val y = scroll.scrollY
        content.removeAllViews()
        content.body()
        if (keepScroll) scroll.post { scroll.scrollTo(0, y) } else scroll.scrollTo(0, 0)
    }

    fun toast(msg: String, long: Boolean = false) =
        Toast.makeText(this, msg, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()

    // ------------------------------------------------------------ segundo plano

    /** Ejecuta [work] en segundo plano y entrega el resultado en la UI (si la pantalla sigue viva). */
    fun <T> bg(work: () -> T, done: (Result<T>) -> Unit) {
        EXECUTOR.execute {
            val r = runCatching(work)
            main.post { if (!isFinishing && !isDestroyed) done(r) }
        }
    }

    fun postDelayed(ms: Long, r: Runnable) = main.postDelayed(r, ms)
    fun cancel(r: Runnable) = main.removeCallbacks(r)

    // ------------------------------------------------------------ diálogos

    fun confirm(title: String, message: String, confirmText: String, cancelText: String = "Cancelar", onCancel: (() -> Unit)? = null, onYes: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(confirmText) { _, _ -> onYes() }
            .setNegativeButton(cancelText) { _, _ -> onCancel?.invoke() }
            .setCancelable(onCancel == null)
            .show()
    }

    /** Diálogo con botones grandes verticales. */
    fun choiceDialog(title: String, message: String?, options: List<Pair<String, () -> Unit>>, primaryFirst: Boolean = true): AlertDialog {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }
        if (message != null) box.text(message, 15f, color = Palette.MUTED, gapDp = 12)
        lateinit var dlg: AlertDialog
        options.forEachIndexed { i, (label, action) ->
            box.bigButton(label, primary = primaryFirst && i == 0, gapDp = 10) { dlg.dismiss(); action() }
        }
        dlg = AlertDialog.Builder(this).setTitle(title).setView(ScrollView(this).apply { addView(box) })
            .setNegativeButton("Cerrar", null).create()
        dlg.show()
        return dlg
    }

    private fun progressDialog(text: String): AlertDialog {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            addView(ProgressBar(this@BaseActivity))
            addView(makeText(this@BaseActivity, text, 17f).apply { setPadding(dp(16), 0, 0, 0) })
        }
        return AlertDialog.Builder(this).setView(box).setCancelable(false).show()
    }

    // ------------------------------------------------------------ resultados de otras apps

    private val resultCallbacks = mutableMapOf<Int, (Int, Intent?) -> Unit>()
    private var nextRequest = 7000

    fun startForResult(intent: Intent, cb: (resultCode: Int, data: Intent?) -> Unit): Boolean {
        val code = nextRequest++
        resultCallbacks[code] = cb
        return try {
            startActivityForResult(intent, code)
            true
        } catch (e: ActivityNotFoundException) {
            resultCallbacks.remove(code)
            false
        }
    }

    @Deprecated("API de plataforma")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val cb = resultCallbacks.remove(requestCode)
        if (cb != null) cb(resultCode, data) else onUnhandledResult(requestCode, resultCode, data)
    }

    open fun onUnhandledResult(requestCode: Int, resultCode: Int, data: Intent?) {}

    // ------------------------------------------------------------ exportación

    /** Genera un archivo en segundo plano y ofrece: Compartir / Guardar en el teléfono / Abrir. */
    fun runExport(generate: () -> ExportedFile) {
        val progress = progressDialog("Generando archivo…")
        bg(generate) { r ->
            progress.dismiss()
            r.onSuccess { showExportResult(it) }
                .onFailure { e -> AlertDialog.Builder(this).setTitle("No se pudo exportar").setMessage(e.message ?: e.toString()).setPositiveButton("Aceptar", null).show() }
        }
    }

    private fun showExportResult(exp: ExportedFile) {
        val m = c.exports
        choiceDialog("Archivo listo", exp.file.name, listOf(
            "Compartir" to { startActivity(m.shareIntent(exp)) },
            "Guardar en el teléfono" to {
                val i = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = exp.mimeType
                    putExtra(Intent.EXTRA_TITLE, exp.file.name)
                }
                val ok = startForResult(i) { code, data ->
                    val uri = data?.data
                    if (code == RESULT_OK && uri != null) {
                        bg({ m.copyTo(exp, uri) }) { res -> toast(if (res.getOrDefault(false)) "Archivo guardado." else "No se pudo guardar.", true) }
                    }
                }
                if (!ok) toast("No hay un gestor de archivos disponible.", true)
            },
            "Abrir" to {
                try { startActivity(m.viewIntent(exp)) } catch (e: ActivityNotFoundException) {
                    toast("No hay una app instalada para abrir este archivo.", true)
                }
            },
        ))
    }

    fun mimeIsPdf(exp: ExportedFile) = exp.mimeType == ExportManager.MIME_PDF

    companion object {
        private val EXECUTOR = Executors.newFixedThreadPool(2)
    }
}
