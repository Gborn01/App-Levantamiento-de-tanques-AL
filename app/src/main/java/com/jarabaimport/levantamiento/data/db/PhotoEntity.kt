package com.jarabaimport.levantamiento.data.db

import java.util.UUID

/** Fotografía asociada a un tanque. El archivo JPEG vive en el almacenamiento interno de la app. */
data class PhotoEntity(
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val tankId: Long,
    /** Código de [com.jarabaimport.levantamiento.domain.model.PhotoCategory]. */
    val category: String,
    val filePath: String,
    val caption: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
