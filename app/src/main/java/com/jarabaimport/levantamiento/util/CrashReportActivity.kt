package com.jarabaimport.levantamiento.util

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Pantalla mínima (sin depender del resto de la app) que muestra el último error
 * y permite enviarlo por WhatsApp/correo para poder corregirlo.
 */
class CrashReportActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val report = intent.getStringExtra(EXTRA_REPORT) ?: "Sin información."
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(pad, pad * 2, pad, pad)
        root.setBackgroundColor(Color.WHITE)

        val title = TextView(this)
        title.text = "La app tuvo un error"
        title.textSize = 22f
        title.setTextColor(0xFF002C77.toInt())
        root.addView(title)

        val info = TextView(this)
        info.text = "Pulse ENVIAR INFORME y mándelo al desarrollador. Luego pulse CONTINUAR."
        info.textSize = 16f
        info.setTextColor(Color.DKGRAY)
        info.setPadding(0, pad / 2, 0, pad / 2)
        root.addView(info)

        val send = Button(this)
        send.text = "ENVIAR INFORME"
        send.setOnClickListener {
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_SUBJECT, "Error – Levantamiento Tanques")
            i.putExtra(Intent.EXTRA_TEXT, report)
            startActivity(Intent.createChooser(i, "Enviar informe"))
        }
        root.addView(send)

        val cont = Button(this)
        cont.text = "CONTINUAR"
        cont.setOnClickListener {
            startActivity(Intent(this, com.jarabaimport.levantamiento.ui.home.MainActivity::class.java))
            finish()
        }
        root.addView(cont)

        val body = TextView(this)
        body.text = report
        body.textSize = 12f
        body.setTextColor(Color.BLACK)
        body.setTextIsSelectable(true)
        val sv = ScrollView(this)
        sv.addView(body)
        root.addView(sv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    companion object {
        const val EXTRA_REPORT = "report"
    }
}
