package com.jarabaimport.levantamiento.ui.clients

import android.os.Bundle
import android.text.InputType
import com.jarabaimport.levantamiento.data.db.ClientEntity
import com.jarabaimport.levantamiento.ui.BaseActivity
import com.jarabaimport.levantamiento.ui.Field
import com.jarabaimport.levantamiento.ui.Nav
import com.jarabaimport.levantamiento.ui.bigButton
import com.jarabaimport.levantamiento.ui.textField

/** Crear / editar cliente. El nombre es obligatorio. */
class ClientFormActivity : BaseActivity() {

    private var client = ClientEntity()
    private val fields = mutableMapOf<String, Field>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getLongExtra(Nav.EXTRA_CLIENT, 0L)
        if (id != 0L) c.clients.get(id)?.let { client = it }
        setupScreen(if (client.id == 0L) "Nuevo cliente" else "Editar cliente", client.name.ifBlank { null })
        content.apply {
            fields["name"] = textField("Nombre del cliente", client.name, required = true) { v -> client = client.copy(name = v); revalidate() }
            textField("Contacto", client.contact) { v -> client = client.copy(contact = v) }
            textField("Teléfono", client.phone, inputType = InputType.TYPE_CLASS_PHONE) { v -> client = client.copy(phone = v) }
            fields["email"] = textField("Email", client.email, inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) { v ->
                client = client.copy(email = v); revalidate()
            }
            textField("Dirección", client.address) { v -> client = client.copy(address = v) }
            textField("Ciudad", client.city) { v -> client = client.copy(city = v) }
            textField("Notas", client.notes, multiline = true) { v -> client = client.copy(notes = v) }
            bigButton("Guardar cliente") { save() }
        }
    }

    private var showErrors = false

    private fun revalidate() { if (showErrors) applyErrors(validate(client)) }

    private fun applyErrors(e: Map<String, String>) = fields.forEach { (k, f) -> f.setError(e[k]) }

    private fun save() {
        showErrors = true
        val e = validate(client)
        applyErrors(e)
        if (e.isNotEmpty()) {
            toast(e.values.first(), true)
            return
        }
        val isNew = client.id == 0L
        val id = c.clients.save(client)
        toast("Cliente guardado.")
        if (isNew) Nav.client(this, id)
        finish()
    }

    companion object {
        fun validate(cl: ClientEntity): Map<String, String> = buildMap<String, String> {
            if (cl.name.isBlank()) put("name", "El nombre del cliente es obligatorio.")
            if (cl.email.isNotBlank() && !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(cl.email.trim())) {
                put("email", "Correo electrónico no válido.")
            }
        }
    }
}
