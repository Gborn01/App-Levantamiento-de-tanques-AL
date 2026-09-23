package com.jarabaimport.levantamiento.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.jarabaimport.levantamiento.data.db.AppDatabase
import com.jarabaimport.levantamiento.data.repository.FileStore
import com.jarabaimport.levantamiento.domain.Fmt
import com.jarabaimport.levantamiento.util.AppFileProvider
import java.io.File

/** Archivo exportado listo para compartir/guardar. */
data class ExportedFile(val file: File, val mimeType: String)

/**
 * Orquesta la exportación (PDF/CSV). Los archivos se crean en la caché de la app
 * y se comparten mediante [AppFileProvider], o el usuario los guarda donde quiera.
 * Todas las funciones de generación deben llamarse en segundo plano.
 */
class ExportManager(
    private val context: Context,
    private val db: AppDatabase,
    private val files: FileStore,
) {
    private val pdf = PdfReportGenerator(context)

    fun tankPdf(tankId: Long): ExportedFile {
        val tank = requireNotNull(db.tankDao().getById(tankId)) { "Tanque no encontrado" }
        val client = requireNotNull(db.clientDao().getById(tank.clientId)) { "Cliente no encontrado" }
        val photos = db.photoDao().getByTank(tankId)
        val name = "Levantamiento_${Fmt.safeFileName(client.name)}_${Fmt.safeFileName(tank.code.ifBlank { tank.name })}_${Fmt.fileStamp()}.pdf"
        val out = File(files.exportsDir, name)
        pdf.generate(listOf(ReportData(client, tank, photos)), out)
        return ExportedFile(out, MIME_PDF)
    }

    fun clientPdf(clientId: Long): ExportedFile {
        val client = requireNotNull(db.clientDao().getById(clientId)) { "Cliente no encontrado" }
        val tanks = db.tankDao().getByClient(clientId)
        require(tanks.isNotEmpty()) { "El cliente no tiene tanques registrados." }
        val reports = tanks.map { ReportData(client, it, db.photoDao().getByTank(it.id)) }
        val out = File(files.exportsDir, "Levantamientos_${Fmt.safeFileName(client.name)}_${Fmt.fileStamp()}.pdf")
        pdf.generate(reports, out)
        return ExportedFile(out, MIME_PDF)
    }

    fun tankCsv(tankId: Long): ExportedFile {
        val tank = requireNotNull(db.tankDao().getById(tankId)) { "Tanque no encontrado" }
        val client = requireNotNull(db.clientDao().getById(tank.clientId)) { "Cliente no encontrado" }
        val photos = db.photoDao().getByTank(tankId).size
        val out = File(files.exportsDir, "Tanque_${Fmt.safeFileName(client.name)}_${Fmt.safeFileName(tank.code.ifBlank { tank.name })}_${Fmt.fileStamp()}.csv")
        CsvExporter.writeToFile(listOf(Triple(client, tank, photos)), out)
        return ExportedFile(out, MIME_CSV)
    }

    fun clientCsv(clientId: Long): ExportedFile {
        val client = requireNotNull(db.clientDao().getById(clientId)) { "Cliente no encontrado" }
        val rows = db.tankDao().getByClient(clientId).map { Triple(client, it, db.photoDao().getByTank(it.id).size) }
        val out = File(files.exportsDir, "Tanques_${Fmt.safeFileName(client.name)}_${Fmt.fileStamp()}.csv")
        CsvExporter.writeToFile(rows, out)
        return ExportedFile(out, MIME_CSV)
    }

    /** CSV con TODOS los tanques de TODOS los clientes. */
    fun allCsv(): ExportedFile {
        val rows = db.clientDao().getAll().flatMap { client ->
            db.tankDao().getByClient(client.id).map { Triple(client, it, db.photoDao().getByTank(it.id).size) }
        }
        val out = File(files.exportsDir, "Todos_los_tanques_${Fmt.fileStamp()}.csv")
        CsvExporter.writeToFile(rows, out)
        return ExportedFile(out, MIME_CSV)
    }

    fun uriFor(file: File): Uri = AppFileProvider.uriFor(context, file)

    fun shareIntent(export: ExportedFile): Intent {
        val uri = uriFor(export.file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = export.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, export.file.nameWithoutExtension)
            clipData = ClipData.newRawUri(export.file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Compartir ${export.file.name}")
    }

    fun viewIntent(export: ExportedFile): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uriFor(export.file), export.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    /** Copia el archivo a un destino elegido por el usuario (Storage Access Framework). */
    fun copyTo(export: ExportedFile, destination: Uri): Boolean = try {
        context.contentResolver.openOutputStream(destination)?.use { out ->
            export.file.inputStream().use { it.copyTo(out) }
        } != null
    } catch (e: Exception) {
        false
    }

    companion object {
        const val MIME_PDF = "application/pdf"
        const val MIME_CSV = "text/csv"
    }
}
