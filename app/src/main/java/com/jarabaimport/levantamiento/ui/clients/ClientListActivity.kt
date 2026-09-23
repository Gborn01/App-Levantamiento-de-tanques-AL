package com.jarabaimport.levantamiento.ui.clients

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.widget.EditText
import android.widget.LinearLayout
import com.jarabaimport.levantamiento.domain.Fmt
import com.jarabaimport.levantamiento.ui.BaseActivity
import com.jarabaimport.levantamiento.ui.Nav
import com.jarabaimport.levantamiento.ui.Palette
import com.jarabaimport.levantamiento.ui.addWithGap
import com.jarabaimport.levantamiento.ui.bigButton
import com.jarabaimport.levantamiento.ui.card
import com.jarabaimport.levantamiento.ui.dp
import com.jarabaimport.levantamiento.ui.dpf
import com.jarabaimport.levantamiento.ui.rounded
import com.jarabaimport.levantamiento.ui.sectionTitle
import com.jarabaimport.levantamiento.ui.text
import com.jarabaimport.levantamiento.ui.vertical

/** Lista de clientes con búsqueda por cliente, código de tanque, nombre de tanque o producto. */
class ClientListActivity : BaseActivity() {

    private lateinit var results: LinearLayout
    private var query = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupScreen("Clientes")
        val search = EditText(this).apply {
            hint = "🔍  Buscar cliente, código, tanque o producto"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
            setPadding(dp(16), dp(14), dp(16), dp(14))
            minHeight = dp(56)
            background = rounded(0xFFFFFFFF.toInt(), dpf(28f), Palette.NAVY, dp(2))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) { query = s?.toString()?.trim() ?: ""; renderResults() }
            })
        }
        content.addWithGap(search, 14)
        content.bigButton("+ Nuevo cliente") { Nav.clientForm(this) }
        results = content.vertical(0)
    }

    override fun onResume() {
        super.onResume()
        renderResults()
    }

    private fun renderResults() {
        results.removeAllViews()
        val clients = c.clients.summaries(query)
        val searching = query.isNotBlank()
        if (searching) results.sectionTitle("Clientes (${clients.size})")
        if (clients.isEmpty()) {
            results.text(if (searching) "Ningún cliente coincide." else "No hay clientes. Cree el primero.", color = Palette.MUTED, gapDp = 14)
        }
        clients.forEach { s ->
            results.card(gapDp = 10, onClick = { Nav.client(this, s.id) }) {
                text("Cliente:", 13f, color = Palette.MUTED, gapDp = 0)
                text(s.name, 22f, bold = true, color = Palette.NAVY, gapDp = 4)
                text("Tanques: ${s.tankCount}", 17f, gapDp = 2)
                text("Último levantamiento: ${Fmt.date(s.lastSurveyDate)}", 15f, color = Palette.MUTED, gapDp = 0)
            }
        }
        if (searching) {
            val tanks = c.tanks.search(query)
            results.sectionTitle("Tanques (${tanks.size})")
            if (tanks.isEmpty()) results.text("Ningún tanque coincide.", color = Palette.MUTED)
            tanks.forEach { t -> results.tankCard(t, showClient = true) { Nav.summary(this, t.id) } }
        }
    }
}
