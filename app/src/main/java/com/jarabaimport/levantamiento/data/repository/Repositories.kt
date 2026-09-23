package com.jarabaimport.levantamiento.data.repository

import android.content.Context
import android.net.Uri
import com.jarabaimport.levantamiento.data.db.AppDatabase
import com.jarabaimport.levantamiento.data.db.ClientEntity
import com.jarabaimport.levantamiento.data.db.ClientSummary
import com.jarabaimport.levantamiento.data.db.PhotoEntity
import com.jarabaimport.levantamiento.data.db.TankEntity
import com.jarabaimport.levantamiento.data.db.TankListItem
import com.jarabaimport.levantamiento.domain.KeyData
import com.jarabaimport.levantamiento.domain.TankDuplicator
import com.jarabaimport.levantamiento.domain.model.PhotoCategory
import com.jarabaimport.levantamiento.util.ImageUtils
import java.io.File
import java.util.UUID

/*
 * Repositorios: única puerta de entrada de la UI a los datos.
 * Las operaciones sobre SQLite son rápidas (ms) y se pueden llamar desde la UI;
 * el procesamiento de imágenes y la exportación se ejecutan en segundo plano (ver ui/Bg).
 */

class ClientRepository(private val db: AppDatabase, private val files: FileStore) {
    private val dao get() = db.clientDao()

    fun summaries(query: String = ""): List<ClientSummary> = dao.summaries(query)
    fun get(id: Long): ClientEntity? = dao.getById(id)
    fun count(): Int = dao.count()
    fun all(): List<ClientEntity> = dao.getAll()

    /** Inserta o actualiza. Devuelve el id. */
    fun save(client: ClientEntity): Long {
        require(client.name.isNotBlank()) { "El nombre del cliente es obligatorio." }
        val now = System.currentTimeMillis()
        return if (client.id == 0L) {
            dao.insert(client.copy(name = client.name.trim(), createdAt = now, updatedAt = now))
        } else {
            dao.update(client.copy(name = client.name.trim(), updatedAt = now))
            client.id
        }
    }

    /** Borra el cliente, sus tanques y fotos (CASCADE) y los archivos asociados. */
    fun delete(clientId: Long) {
        val tanks = db.tankDao().getByClient(clientId)
        val photos = db.photoDao().getByClient(clientId)
        dao.delete(clientId)
        photos.forEach { File(it.filePath).delete() }
        tanks.forEach {
            if (it.signaturePath.isNotBlank()) File(it.signaturePath).delete()
            files.tankPhotoDir(it.id).deleteRecursively()
        }
    }
}

class TankRepository(private val db: AppDatabase, private val files: FileStore) {
    private val dao get() = db.tankDao()

    fun get(id: Long): TankEntity? = dao.getById(id)
    fun byClient(clientId: Long): List<TankEntity> = dao.getByClient(clientId)
    fun recent(limit: Int = 100): List<TankListItem> = dao.recent(limit)
    fun search(query: String): List<TankListItem> = dao.search(query)
    fun count(): Int = dao.count()
    fun completedCount(): Int = dao.completedCount()

    /**
     * Guarda el tanque recalculando el estado automático (Borrador / Completado).
     * Devuelve el id (nuevo o existente).
     */
    fun save(tank: TankEntity): Long {
        val now = System.currentTimeMillis()
        val withStatus = tank.copy(status = KeyData.autoStatus(tank), updatedAt = now)
        return if (tank.id == 0L) {
            dao.insert(withStatus.copy(createdAt = now))
        } else {
            dao.update(withStatus)
            tank.id
        }
    }

    /** Cambia el estado manualmente (Pendiente de revisión / Revisado). */
    fun setStatus(id: Long, statusCode: String) {
        val t = dao.getById(id) ?: return
        dao.update(t.copy(status = statusCode, updatedAt = System.currentTimeMillis()))
    }

    /** Vuelve al estado automático (Borrador / Completado según los 7 datos). */
    fun resetAutoStatus(id: Long) {
        val t = dao.getById(id) ?: return
        save(t.copy(status = "DRAFT"))
    }

    fun duplicate(id: Long): Long? {
        val src = dao.getById(id) ?: return null
        return save(TankDuplicator.duplicate(src, dao.codesForClient(src.clientId)))
    }

    fun suggestNextCode(clientId: Long): String {
        val codes = dao.codesForClient(clientId)
        if (codes.isEmpty()) return "TK-001"
        val numbered = codes.filter { Regex("\\d+$").containsMatchIn(it) }
        val last = numbered.maxByOrNull { Regex("(\\d+)$").find(it)?.groupValues?.get(1)?.toLongOrNull() ?: 0L }
            ?: return "TK-" + (codes.size + 1).toString().padStart(3, '0')
        return TankDuplicator.nextCode(last, codes)
    }

    fun delete(tankId: Long) {
        val tank = dao.getById(tankId) ?: return
        val photos = db.photoDao().getByTank(tankId)
        dao.delete(tankId)
        photos.forEach { File(it.filePath).delete() }
        if (tank.signaturePath.isNotBlank()) File(tank.signaturePath).delete()
        files.tankPhotoDir(tankId).deleteRecursively()
    }
}

class PhotoRepository(private val db: AppDatabase, private val files: FileStore) {
    private val dao get() = db.photoDao()

    fun byTank(tankId: Long): List<PhotoEntity> = dao.getByTank(tankId)

    /** Normaliza la imagen (rotación, tamaño) al almacenamiento interno y la registra. Llamar en segundo plano. */
    fun addFromUri(context: Context, tankId: Long, category: PhotoCategory, uri: Uri): Boolean {
        val dest = File(files.tankPhotoDir(tankId), "${category.code}_${UUID.randomUUID()}.jpg")
        val ok = ImageUtils.normalizeToJpeg(context, uri, dest)
        if (ok) dao.insert(PhotoEntity(tankId = tankId, category = category.code, filePath = dest.absolutePath))
        return ok
    }

    fun delete(photo: PhotoEntity) {
        dao.delete(photo.id)
        File(photo.filePath).delete()
    }
}

/** Rutas de archivos de la app (almacenamiento interno, privado, sin permisos). */
class FileStore(private val context: Context) {
    val photosRoot: File get() = File(context.filesDir, "photos").apply { mkdirs() }
    fun tankPhotoDir(tankId: Long) = File(photosRoot, "tank_$tankId").apply { mkdirs() }
    fun signatureFile(tankId: Long) =
        File(File(context.filesDir, "signatures").apply { mkdirs() }, "signature_${tankId}_${System.currentTimeMillis()}.png")
    val exportsDir: File get() = File(context.cacheDir, "exports").apply { mkdirs() }
    val cameraTempDir: File get() = File(context.cacheDir, "camera").apply { mkdirs() }
}
