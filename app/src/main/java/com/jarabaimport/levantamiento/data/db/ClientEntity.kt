package com.jarabaimport.levantamiento.data.db

import java.util.UUID

/** Cliente. Un cliente tiene N tanques. El nombre es obligatorio. */
data class ClientEntity(
    val id: Long = 0,
    /** Identificador único global (útil para una futura sincronización/exportación). */
    val uuid: String = UUID.randomUUID().toString(),
    val name: String = "",
    val contact: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Fila para la lista de clientes (con agregados). */
data class ClientSummary(
    val id: Long,
    val name: String,
    val city: String,
    val tankCount: Int,
    val lastSurveyDate: Long?,
)

/** Fila para listas de tanques (búsqueda, recientes). */
data class TankListItem(
    val id: Long,
    val clientId: Long,
    val clientName: String,
    val name: String,
    val code: String,
    val product: String,
    val nominalVolumeL: Double?,
    val surveyDate: Long,
    val status: String,
    val updatedAt: Long,
)
