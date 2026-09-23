package com.jarabaimport.levantamiento.data.db

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Base de datos local SQLite (nativa de Android). Todo funciona sin Internet.
 *
 * Relaciones: Cliente 1 → N Tanques (ON DELETE CASCADE), Tanque 1 → N Fotografías (ON DELETE CASCADE).
 *
 * Para cambiar el esquema: subir [VERSION] y añadir el paso correspondiente en [onUpgrade]
 * (p.ej. "ALTER TABLE tanks ADD COLUMN ..."). Nunca borrar tablas con datos del usuario.
 */
class DbHelper(context: Context, name: String? = NAME) : SQLiteOpenHelper(context, name, null, VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE clients (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL UNIQUE,
                name TEXT NOT NULL,
                contact TEXT NOT NULL DEFAULT '',
                phone TEXT NOT NULL DEFAULT '',
                email TEXT NOT NULL DEFAULT '',
                address TEXT NOT NULL DEFAULT '',
                city TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_clients_name ON clients(name)")
        db.execSQL("CREATE TABLE tanks (\n" + TANK_COLUMNS_SQL.joinToString(",\n") + "\n)")
        db.execSQL("CREATE INDEX idx_tanks_client ON tanks(clientId)")
        db.execSQL("CREATE INDEX idx_tanks_code ON tanks(code)")
        db.execSQL("CREATE INDEX idx_tanks_product ON tanks(product)")
        db.execSQL(
            """
            CREATE TABLE photos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL UNIQUE,
                tankId INTEGER NOT NULL REFERENCES tanks(id) ON DELETE CASCADE,
                category TEXT NOT NULL,
                filePath TEXT NOT NULL,
                caption TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_photos_tank ON photos(tankId)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // V1: sin migraciones todavía. Ejemplo futuro:
        // if (oldVersion < 2) db.execSQL("ALTER TABLE tanks ADD COLUMN nuevoCampo TEXT NOT NULL DEFAULT ''")
    }

    companion object {
        const val NAME = "levantamiento.db"
        const val VERSION = 1
    }
}

// ---- Utilidades de lectura de Cursor ----
internal fun Cursor.idx(col: String) = getColumnIndexOrThrow(col)
internal fun Cursor.str(col: String): String = getString(idx(col)) ?: ""
internal fun Cursor.long(col: String): Long = getLong(idx(col))
internal fun Cursor.int(col: String): Int = getInt(idx(col))
internal fun Cursor.bool(col: String): Boolean = getInt(idx(col)) != 0
internal fun Cursor.dblOrNull(col: String): Double? = idx(col).let { if (isNull(it)) null else getDouble(it) }
internal fun Cursor.intOrNull(col: String): Int? = idx(col).let { if (isNull(it)) null else getInt(it) }
internal fun Cursor.longOrNull(col: String): Long? = idx(col).let { if (isNull(it)) null else getLong(it) }
internal fun Cursor.boolOrNull(col: String): Boolean? = idx(col).let { if (isNull(it)) null else getInt(it) != 0 }

internal inline fun <T> Cursor.mapAll(f: (Cursor) -> T): List<T> = use { c ->
    val out = ArrayList<T>(c.count)
    while (c.moveToNext()) out.add(f(c))
    out
}
