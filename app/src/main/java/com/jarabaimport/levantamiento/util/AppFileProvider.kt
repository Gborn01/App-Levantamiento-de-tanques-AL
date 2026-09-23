package com.jarabaimport.levantamiento.util

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException

/**
 * Proveedor de archivos mínimo (equivalente a FileProvider, sin dependencias).
 * Expone de forma temporal y controlada:
 *  - exports/  → PDF/CSV generados (caché), para compartir o abrir.
 *  - camera/   → archivo temporal donde la app de cámara escribe la foto.
 * Los permisos se conceden por Intent (FLAG_GRANT_*_URI_PERMISSION); no es exportado.
 */
class AppFileProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    private fun roots(ctx: Context) = mapOf(
        "exports" to File(ctx.cacheDir, "exports"),
        "camera" to File(ctx.cacheDir, "camera"),
    )

    private fun fileFor(uri: Uri): File {
        val ctx = context ?: throw FileNotFoundException()
        val segs = uri.pathSegments
        if (segs.size < 2) throw FileNotFoundException(uri.toString())
        val root = roots(ctx)[segs[0]] ?: throw FileNotFoundException(uri.toString())
        val f = File(root, segs.drop(1).joinToString("/")).canonicalFile
        if (!f.path.startsWith(root.canonicalPath)) throw SecurityException("Ruta no permitida")
        return f
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val f = fileFor(uri)
        if (mode.contains('w')) f.parentFile?.mkdirs()
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.parseMode(mode))
    }

    override fun getType(uri: Uri): String {
        val ext = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase() ?: ""
        return when (ext) {
            "pdf" -> "application/pdf"
            "csv" -> "text/csv"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val f = fileFor(uri)
        val cols = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val row = cols.map { c ->
            when (c) {
                OpenableColumns.DISPLAY_NAME -> f.name
                OpenableColumns.SIZE -> f.length()
                else -> null
            }
        }.toTypedArray()
        return MatrixCursor(cols, 1).apply { addRow(row) }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        fun authority(context: Context) = context.packageName + ".files"

        /** Uri para un archivo dentro de cacheDir/exports o cacheDir/camera. */
        fun uriFor(context: Context, file: File): Uri {
            val root = when (file.parentFile?.name) {
                "exports" -> "exports"
                "camera" -> "camera"
                else -> throw IllegalArgumentException("Carpeta no compartible: ${file.parent}")
            }
            return Uri.Builder().scheme("content").authority(authority(context))
                .appendPath(root).appendPath(file.name).build()
        }
    }
}
