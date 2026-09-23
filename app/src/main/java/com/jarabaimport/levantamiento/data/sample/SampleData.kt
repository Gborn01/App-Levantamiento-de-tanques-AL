package com.jarabaimport.levantamiento.data.sample

import com.jarabaimport.levantamiento.data.db.ClientEntity
import com.jarabaimport.levantamiento.data.repository.ClientRepository
import com.jarabaimport.levantamiento.data.repository.SettingsRepository
import com.jarabaimport.levantamiento.data.repository.TankRepository

/**
 * Datos de prueba. Se cargan UNA sola vez en la primera ejecución.
 * Se pueden borrar desde la app (borrar cliente) sin afectar nada más.
 */
object SampleData {

    fun seedIfNeeded(settings: SettingsRepository, clients: ClientRepository, tanks: TankRepository) {
        if (settings.sampleDataSeeded) return
        seed(clients, tanks)
        settings.sampleDataSeeded = true
    }

    fun seed(clients: ClientRepository, tanks: TankRepository): Long {
        val clientId = clients.save(
            ClientEntity(
                name = "Bepensa",
                contact = "Ing. de Planta (ejemplo)",
                phone = "809-000-0000",
                email = "planta@ejemplo.com",
                address = "Dirección de ejemplo",
                city = "Santo Domingo",
                notes = "Cliente de ejemplo creado automáticamente. Puede borrarse.",
            )
        )
        tanks.save(sampleTank(clientId))
        return clientId
    }

    fun sampleTank(clientId: Long) = SampleTank.bepensaTk001(clientId)
}
