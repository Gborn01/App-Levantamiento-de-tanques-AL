package com.jarabaimport.levantamiento.ui.clients

import android.os.Bundle
import com.jarabaimport.levantamiento.ui.BaseActivity
import com.jarabaimport.levantamiento.ui.Nav
import com.jarabaimport.levantamiento.ui.Palette
import com.jarabaimport.levantamiento.ui.bigButton
import com.jarabaimport.levantamiento.ui.card
import com.jarabaimport.levantamiento.ui.labeledValue
import com.jarabaimport.levantamiento.ui.makeButton
import com.jarabaimport.levantamiento.ui.row
import com.jarabaimport.levantamiento.ui.sectionTitle
import com.jarabaimport.levantamiento.ui.text
import com.jarabaimport.levantamiento.ui.weighted

/** Detalle del cliente: datos, NUEVO TANQUE, EDITAR CLIENTE, EXPORTAR y lista de tanques. */
class ClientDetailActivity : BaseActivity() {

    private var clientId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clientId = intent.getLongExtra(Nav.EXTRA_CLIENT, 0L)
        setupScreen("Cliente", "Detalle del cliente")
        addHeaderAction("BORRAR") { confirmDelete() }
    }

    override fun onResume() {
        super.onResume()
        val client = c.clients.get(clientId) ?: run { finish(); return }
        val tanks = c.tanks.byClient(clientId)
        setTitles(client.name, "Detalle del cliente")
        rebuild {
            card {
                text(client.name, 24f, bold = true, color = Palette.NAVY, gapDp = 12)
                labeledValue("Contacto", client.contact)
                labeledValue("Dirección", listOf(client.address, client.city).filter { it.isNotBlank() }.joinToString(", "))
                labeledValue("Teléfono", client.phone)
                labeledValue("Email", client.email)
                if (client.notes.isNotBlank()) labeledValue("Notas", client.notes)
            }
            bigButton("+ Nuevo tanque") { Nav.wizard(this@ClientDetailActivity, clientId) }
            row {
                weighted(makeButton(context, "Editar cliente", primary = false) { Nav.clientForm(this@ClientDetailActivity, clientId) })
                weighted(makeButton(context, "Exportar", primary = false) { exportMenu(tanks.size) })
            }
            sectionTitle("Tanques (${tanks.size})")
            if (tanks.isEmpty()) text("Este cliente aún no tiene tanques.", color = Palette.MUTED)
            tanks.forEach { t -> tankCard(t) { Nav.summary(this@ClientDetailActivity, t.id) } }
        }
    }

    private fun exportMenu(count: Int) {
        if (count == 0) { toast("El cliente no tiene tanques para exportar."); return }
        choiceDialog("Exportar cliente", "Incluye los $count tanque(s) del cliente.", listOf(
            "Informe PDF" to { runExport { c.exports.clientPdf(clientId) } },
            "Datos CSV" to { runExport { c.exports.clientCsv(clientId) } },
        ))
    }

    private fun confirmDelete() {
        val n = c.tanks.byClient(clientId).size
        confirm("Borrar cliente", "Se borrarán el cliente, sus $n tanque(s) y todas sus fotografías. No se puede deshacer.", "Borrar") {
            c.clients.delete(clientId)
            if (c.tanks.get(c.settings.pendingTankId) == null) c.settings.clearPending()
            toast("Cliente borrado.")
            finish()
        }
    }
}
