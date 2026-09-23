package com.jarabaimport.levantamiento.data.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/** Acceso a datos de clientes. */
class ClientDao(private val db: SQLiteDatabase) {

    private fun ClientEntity.values() = ContentValues().apply {
        put("uuid", uuid); put("name", name); put("contact", contact); put("phone", phone)
        put("email", email); put("address", address); put("city", city); put("notes", notes)
        put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    private fun read(c: android.database.Cursor) = ClientEntity(
        id = c.long("id"), uuid = c.str("uuid"), name = c.str("name"), contact = c.str("contact"),
        phone = c.str("phone"), email = c.str("email"), address = c.str("address"), city = c.str("city"),
        notes = c.str("notes"), createdAt = c.long("createdAt"), updatedAt = c.long("updatedAt"),
    )

    fun insert(c: ClientEntity): Long = db.insertOrThrow("clients", null, c.values())
    fun update(c: ClientEntity) { db.update("clients", c.values(), "id = ?", arrayOf(c.id.toString())) }
    fun delete(id: Long) { db.delete("clients", "id = ?", arrayOf(id.toString())) }

    fun getById(id: Long): ClientEntity? =
        db.rawQuery("SELECT * FROM clients WHERE id = ?", arrayOf(id.toString())).mapAll(::read).firstOrNull()

    fun getAll(): List<ClientEntity> =
        db.rawQuery("SELECT * FROM clients ORDER BY name COLLATE NOCASE ASC", null).mapAll(::read)

    fun count(): Int = db.rawQuery("SELECT COUNT(*) FROM clients", null).mapAll { it.getInt(0) }.first()

    fun summaries(query: String): List<ClientSummary> {
        val q = query.trim()
        return db.rawQuery(
            """
            SELECT c.id AS id, c.name AS name, c.city AS city,
                   COUNT(t.id) AS tankCount, MAX(t.surveyDate) AS lastSurveyDate
            FROM clients c LEFT JOIN tanks t ON t.clientId = c.id
            WHERE (? = '' OR c.name LIKE '%' || ? || '%')
            GROUP BY c.id
            ORDER BY c.name COLLATE NOCASE ASC
            """.trimIndent(), arrayOf(q, q)
        ).mapAll {
            ClientSummary(it.long("id"), it.str("name"), it.str("city"), it.int("tankCount"), it.longOrNull("lastSurveyDate"))
        }
    }
}

/** Acceso a datos de tanques. */
class TankDao(private val db: SQLiteDatabase) {

    fun insert(t: TankEntity): Long = db.insertOrThrow("tanks", null, t.toValues())
    fun update(t: TankEntity) { db.update("tanks", t.toValues(), "id = ?", arrayOf(t.id.toString())) }
    fun delete(id: Long) { db.delete("tanks", "id = ?", arrayOf(id.toString())) }

    fun getById(id: Long): TankEntity? =
        db.rawQuery("SELECT * FROM tanks WHERE id = ?", arrayOf(id.toString())).mapAll { it.toTank() }.firstOrNull()

    fun getByClient(clientId: Long): List<TankEntity> = db.rawQuery(
        "SELECT * FROM tanks WHERE clientId = ? ORDER BY code COLLATE NOCASE ASC, name COLLATE NOCASE ASC",
        arrayOf(clientId.toString())
    ).mapAll { it.toTank() }

    fun codesForClient(clientId: Long): List<String> =
        db.rawQuery("SELECT code FROM tanks WHERE clientId = ?", arrayOf(clientId.toString())).mapAll { it.getString(0) ?: "" }

    fun count(): Int = db.rawQuery("SELECT COUNT(*) FROM tanks", null).mapAll { it.getInt(0) }.first()

    fun completedCount(): Int =
        db.rawQuery("SELECT COUNT(*) FROM tanks WHERE status != 'DRAFT'", null).mapAll { it.getInt(0) }.first()

    private val listSelect = """
        SELECT t.id AS id, t.clientId AS clientId, c.name AS clientName, t.name AS name,
               t.code AS code, t.product AS product, t.nominalVolumeL AS nominalVolumeL,
               t.surveyDate AS surveyDate, t.status AS status, t.updatedAt AS updatedAt
        FROM tanks t INNER JOIN clients c ON c.id = t.clientId
    """.trimIndent()

    private fun readItem(c: android.database.Cursor) = TankListItem(
        c.long("id"), c.long("clientId"), c.str("clientName"), c.str("name"), c.str("code"), c.str("product"),
        c.dblOrNull("nominalVolumeL"), c.long("surveyDate"), c.str("status"), c.long("updatedAt"),
    )

    fun recent(limit: Int): List<TankListItem> =
        db.rawQuery("$listSelect ORDER BY t.updatedAt DESC LIMIT $limit", null).mapAll(::readItem)

    /** Busca por código, nombre de tanque, producto o nombre de cliente. */
    fun search(query: String): List<TankListItem> {
        val q = query.trim()
        return db.rawQuery(
            """
            $listSelect
            WHERE t.code LIKE '%' || ? || '%' OR t.name LIKE '%' || ? || '%'
               OR t.product LIKE '%' || ? || '%' OR c.name LIKE '%' || ? || '%'
            ORDER BY c.name COLLATE NOCASE ASC, t.code COLLATE NOCASE ASC
            LIMIT 200
            """.trimIndent(), arrayOf(q, q, q, q)
        ).mapAll(::readItem)
    }
}

/** Acceso a datos de fotografías. */
class PhotoDao(private val db: SQLiteDatabase) {

    private fun read(c: android.database.Cursor) = PhotoEntity(
        id = c.long("id"), uuid = c.str("uuid"), tankId = c.long("tankId"), category = c.str("category"),
        filePath = c.str("filePath"), caption = c.str("caption"), createdAt = c.long("createdAt"),
    )

    fun insert(p: PhotoEntity): Long = db.insertOrThrow("photos", null, ContentValues().apply {
        put("uuid", p.uuid); put("tankId", p.tankId); put("category", p.category)
        put("filePath", p.filePath); put("caption", p.caption); put("createdAt", p.createdAt)
    })

    fun delete(id: Long) { db.delete("photos", "id = ?", arrayOf(id.toString())) }

    fun getByTank(tankId: Long): List<PhotoEntity> =
        db.rawQuery("SELECT * FROM photos WHERE tankId = ? ORDER BY createdAt ASC", arrayOf(tankId.toString())).mapAll(::read)

    fun getByClient(clientId: Long): List<PhotoEntity> = db.rawQuery(
        "SELECT p.* FROM photos p INNER JOIN tanks t ON t.id = p.tankId WHERE t.clientId = ?",
        arrayOf(clientId.toString())
    ).mapAll(::read)
}

/** Punto de acceso único a la base de datos. */
class AppDatabase(private val helper: DbHelper) {
    private val db: SQLiteDatabase get() = helper.writableDatabase
    fun clientDao() = ClientDao(db)
    fun tankDao() = TankDao(db)
    fun photoDao() = PhotoDao(db)
    fun close() = helper.close()

    companion object {
        fun build(context: android.content.Context) = AppDatabase(DbHelper(context.applicationContext))
        /** Base de datos en memoria para pruebas. */
        fun inMemory(context: android.content.Context) = AppDatabase(DbHelper(context.applicationContext, null))
    }
}
