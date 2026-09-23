package com.jarabaimport.levantamiento.ui.home

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.jarabaimport.levantamiento.export.BrandingConfig
import com.jarabaimport.levantamiento.ui.BaseActivity
import com.jarabaimport.levantamiento.ui.Nav
import com.jarabaimport.levantamiento.ui.Palette
import com.jarabaimport.levantamiento.ui.bigButton
import com.jarabaimport.levantamiento.ui.dp
import com.jarabaimport.levantamiento.ui.dpf
import com.jarabaimport.levantamiento.ui.makeText
import com.jarabaimport.levantamiento.ui.rounded
import com.jarabaimport.levantamiento.ui.row
import com.jarabaimport.levantamiento.ui.spacer
import com.jarabaimport.levantamiento.ui.text
import com.jarabaimport.levantamiento.ui.weighted
import com.jarabaimport.levantamiento.util.CrashReportActivity
import com.jarabaimport.levantamiento.util.CrashReporter

/** Pantalla principal: "LEVANTAMIENTO DE TANQUES". */
class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Si la vez anterior la app se cerró por un error, mostrarlo primero (pantalla independiente).
        val report = CrashReporter.takeLast(this)
        if (report != null) {
            super.onCreate(savedInstanceState)
            startActivity(android.content.Intent(this, CrashReportActivity::class.java).putExtra(CrashReportActivity.EXTRA_REPORT, report))
            finish()
            return
        }
        super.onCreate(savedInstanceState)
        setupScreen("", showBack = false, withHeader = false)
    }

    override fun onResume() {
        super.onResume()
        try {
            render()
        } catch (e: Throwable) {
            CrashReporter.record(this, e, "pantalla de inicio")
            CrashReporter.takeLast(this)?.let { r ->
                startActivity(android.content.Intent(this, CrashReportActivity::class.java).putExtra(CrashReportActivity.EXTRA_REPORT, r))
            }
            return
        }
        checkPending()
    }

    private fun render() = rebuild(keepScroll = false) {
        spacer(12)
        text(BrandingConfig.COMPANY_NAME.uppercase(), 14f, bold = true, color = Palette.ACCENT, gapDp = 4).apply { letterSpacing = 0.15f }
        text("LEVANTAMIENTO\nDE TANQUES", 32f, bold = true, color = Palette.NAVY, gapDp = 6).apply { setLineSpacing(0f, 1.0f) }
        text("Datos de campo para sistemas de limpieza de tanques", 15f, color = Palette.MUTED, gapDp = 12)
        addView(View(context).apply { setBackgroundColor(Palette.ACCENT) },
            LinearLayout.LayoutParams(dp(64), dp(4)).apply { bottomMargin = dp(20) })

        row(20) {
            weighted(stat("Clientes\nregistrados", c.clients.count()))
            weighted(stat("Tanques\nregistrados", c.tanks.count()))
            weighted(stat("Levantamientos\nrealizados", c.tanks.completedCount()))
        }

        bigButton("+ Nuevo cliente") { Nav.clientForm(this@MainActivity) }
        bigButton("Clientes") { Nav.clients(this@MainActivity) }
        bigButton("Levantamientos recientes") { Nav.recent(this@MainActivity) }
        bigButton("Exportar todo (CSV)", primary = false) { runExport { c.exports.allCsv() } }

        text("Funciona sin Internet. Los datos se guardan en este teléfono.", 14f, color = Palette.MUTED, gapDp = 4).apply {
            gravity = Gravity.CENTER
        }
    }

    private fun stat(label: String, value: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(6), dp(14), dp(6), dp(14))
        background = rounded(Palette.SKY, dpf(12f))
        addView(makeText(context, value.toString(), 32f, bold = true, color = Palette.NAVY).apply { gravity = Gravity.CENTER })
        addView(makeText(context, label, 12f, color = Palette.MUTED).apply { gravity = Gravity.CENTER })
    }

    /** "Hay un levantamiento sin terminar. ¿Desea continuar?" (una vez por arranque). */
    private fun checkPending() {
        if (c.pendingPromptChecked) return
        c.pendingPromptChecked = true
        val id = c.settings.pendingTankId
        if (id <= 0) return
        val t = c.tanks.get(id)
        if (t == null) {
            c.settings.clearPending(); return
        }
        val label = listOf(t.code, t.name).filter { it.isNotBlank() }.joinToString(" – ")
        confirm(
            "Levantamiento sin terminar",
            "Hay un levantamiento sin terminar. ¿Desea continuar?\n\n$label",
            confirmText = "Continuar",
            cancelText = "Ahora no",
            onCancel = { c.settings.clearPending() },
        ) { Nav.wizard(this, t.clientId, t.id, c.settings.pendingStep) }
    }

}
