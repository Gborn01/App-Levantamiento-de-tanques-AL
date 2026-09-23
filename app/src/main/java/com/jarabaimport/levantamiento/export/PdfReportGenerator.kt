package com.jarabaimport.levantamiento.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.jarabaimport.levantamiento.data.db.ClientEntity
import com.jarabaimport.levantamiento.data.db.PhotoEntity
import com.jarabaimport.levantamiento.data.db.TankEntity
import com.jarabaimport.levantamiento.domain.Fmt
import com.jarabaimport.levantamiento.domain.KeyData
import com.jarabaimport.levantamiento.domain.TankCalculations
import com.jarabaimport.levantamiento.domain.model.HeadPosition
import com.jarabaimport.levantamiento.domain.model.PhotoCategory
import com.jarabaimport.levantamiento.domain.model.PressureSource
import com.jarabaimport.levantamiento.domain.model.ResidueCharacteristic
import com.jarabaimport.levantamiento.domain.model.SurveyStatus
import com.jarabaimport.levantamiento.util.ImageUtils
import java.io.File
import java.io.FileOutputStream

/** Datos de un informe (un tanque). */
data class ReportData(val client: ClientEntity, val tank: TankEntity, val photos: List<PhotoEntity>)

/**
 * Genera el informe PDF con la API nativa de Android (android.graphics.pdf),
 * sin librerías externas y sin Internet. Tamaño A4.
 */
class PdfReportGenerator(private val context: Context) {

    fun generate(reports: List<ReportData>, output: File) {
        val doc = PdfDocument()
        try {
            val w = Writer(doc)
            reports.forEachIndexed { index, r ->
                if (index > 0) w.newPage()
                writeReport(w, r)
            }
            w.finish()
            output.parentFile?.mkdirs()
            FileOutputStream(output).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
    }

    // ------------------------------------------------------------------ contenido

    private fun writeReport(w: Writer, r: ReportData) {
        val t = r.tank
        val c = r.client
        w.coverHeader()
        w.infoGrid(
            listOf(
                "Cliente" to c.name,
                "Tanque" to listOf(t.code, t.name).filter { it.isNotBlank() }.joinToString(" – ").ifEmpty { "—" },
                "Fecha" to Fmt.date(t.surveyDate),
                "Técnico" to t.technician.ifBlank { "—" },
                "Área / planta" to t.area.ifBlank { "—" },
                "Estado" to SurveyStatus.fromCode(t.status).label,
            )
        )

        // Siete datos principales, destacados.
        w.section("DATOS PRINCIPALES PARA EVALUACIÓN")
        w.keyData(KeyData.items(t).map { Triple("${it.number}. ${it.label}", it.value, it.complete) })

        w.section("IDENTIFICACIÓN")
        w.rows(
            "Nombre" to t.name,
            "Código" to t.code,
            "Área / planta" to t.area,
            "Proceso" to t.process,
            "Contacto del cliente" to listOf(c.contact, c.phone, c.email).filter { it.isNotBlank() }.joinToString(" · "),
            "Dirección" to listOf(c.address, c.city).filter { it.isNotBlank() }.joinToString(", "),
        )

        w.section("PRODUCTO Y RESIDUO")
        w.rows(
            "Producto procesado" to t.product,
            "Descripción del producto" to t.productDescription,
            "Residuo después del vaciado" to t.residue,
            "Características del residuo" to ResidueCharacteristic.decode(t.residueCharacteristics).joinToString(", ") {
                if (it == ResidueCharacteristic.OTHER && t.residueOther.isNotBlank()) "Otro: ${t.residueOther}" else it.label
            },
            "Observaciones" to t.productObservations,
        )

        w.section("GEOMETRÍA DEL TANQUE")
        val refH = TankCalculations.referenceHeightMm(t.cylindricalHeightMm, t.totalHeightMm)
        val estV = TankCalculations.cylindricalVolumeLiters(t.internalDiameterMm, t.cylindricalHeightMm)
        val hd = TankCalculations.heightToDiameterRatio(refH, t.internalDiameterMm)
        w.rows(
            "Diámetro interno" to Fmt.withUnit(t.internalDiameterMm, "mm", 0),
            "Altura cilíndrica" to Fmt.withUnit(t.cylindricalHeightMm, "mm", 0),
            "Altura total" to Fmt.withUnit(t.totalHeightMm, "mm", 0),
            "Volumen nominal" to volume(t.nominalVolumeL),
            "Volumen de trabajo" to volume(t.workingVolumeL),
            "Tipo de techo" to t.roofType,
            "Tipo de fondo" to t.bottomType,
            "Ángulo del fondo" to Fmt.withUnit(t.bottomAngleDeg, "°", 1),
            "Material" to t.material,
            "Acabado interno" to t.internalFinish,
            "Volumen cilíndrico estimado*" to (estV?.let { volume(it) } ?: "—"),
            "Relación H/D" to (hd?.let { Fmt.number(it, 2) } ?: "—"),
        )
        w.note("* Estimación geométrica V = π·D²/4·H con la altura cilíndrica. No sustituye el volumen nominal del fabricante.")

        w.section("PARÁMETROS CIP")
        val cipElements = buildList {
            if (t.cipHasFilters) add("Filtros")
            if (t.cipHasValves) add("Válvulas")
            if (t.cipHasElbows) add("Codos")
            if (t.cipHasRestrictions) add("Restricciones")
            if (t.cipOtherElements.isNotBlank()) add(t.cipOtherElements)
        }.joinToString(", ")
        w.rows(
            "Caudal disponible" to Fmt.withUnit(t.cipFlowM3h, "m³/h"),
            "Presión disponible en el cabezal" to Fmt.withUnit(t.cipPressureBar, "bar"),
            "Origen del dato de presión" to (PressureSource.fromCode(t.cipPressureSource)?.label ?: ""),
            "Temperatura" to Fmt.withUnit(t.cipTemperatureC, "°C", 1),
            "Concentración NaOH" to Fmt.withUnit(t.naohConcentrationPct, "%"),
            "Concentración ácido" to Fmt.withUnit(t.acidConcentrationPct, "%"),
            "Tiempo total CIP" to Fmt.withUnit(t.cipTimeMin, "min", 0),
            "Diámetro tubería CIP" to Fmt.withUnit(t.cipPipeDiameterIn, "pulg"),
            "Longitud aproximada" to Fmt.withUnit(t.cipPipeLengthM, "m", 1),
            "Otros elementos en la línea" to cipElements,
        )

        w.section("INTERNOS DEL TANQUE")
        w.rows("Resumen de obstáculos" to KeyData.obstaclesText(t))
        if (t.hasAgitator) w.rows(
            "Agitador – diámetro" to Fmt.withUnit(t.agitatorDiameterMm, "mm", 0),
            "Agitador – altura" to Fmt.withUnit(t.agitatorHeightMm, "mm", 0),
            "Agitador – descripción" to t.agitatorDescription,
        )
        if (t.hasCoil) w.rows(
            "Serpentín – tipo" to t.coilType,
            "Serpentín – diámetro" to Fmt.withUnit(t.coilDiameterMm, "mm", 0),
            "Serpentín – dimensiones" to t.coilDimensions,
            "Serpentín – descripción" to t.coilDescription,
        )
        if (t.hasBaffles) w.rows(
            "Baffles – cantidad" to (t.bafflesCount?.toString() ?: "—"),
            "Baffles – dimensiones aprox." to t.bafflesDimensions,
        )
        if (t.hasOtherObstacles) w.rows("Otros obstáculos" to t.otherObstacles)

        w.section("INSTALACIÓN DEL CABEZAL")
        val pos = HeadPosition.fromCode(t.headPosition)
        w.rows(
            "¿Existe cabezal actualmente?" to when (t.hasExistingHead) { true -> "Sí"; false -> "No"; null -> "—" },
            "Modelo actual" to t.currentHeadModel,
            "Ubicación" to t.headLocation,
            "Altura sobre el fondo" to Fmt.withUnit(t.headHeightAboveBottomMm, "mm", 0),
            "Tipo de conexión" to t.headConnectionType,
            "Diámetro de conexión" to t.headConnectionDiameter,
            "Posición" to (if (pos == HeadPosition.OTHER && t.headPositionOther.isNotBlank()) "Otra: ${t.headPositionOther}" else pos?.label ?: ""),
        )

        w.section("OBSERVACIONES")
        w.paragraph(t.observations.ifBlank { "—" })

        w.section("FIRMA")
        w.rows(
            "Técnico responsable" to t.technician,
            "Representante del cliente" to t.clientRepresentative,
        )
        if (t.signaturePath.isNotBlank()) w.image(t.signaturePath, maxHeight = 110f, label = "Firma")

        if (r.photos.isNotEmpty()) {
            w.section("FOTOGRAFÍAS (${r.photos.size})")
            val sorted = r.photos.sortedBy { PhotoCategory.fromCode(it.category).ordinal }
            w.photoGrid(sorted.map { it.filePath to (PhotoCategory.fromCode(it.category).label + if (it.caption.isNotBlank()) " – ${it.caption}" else "") })
        }
    }

    private fun volume(l: Double?): String {
        if (l == null) return "—"
        return "${Fmt.number(l, 0)} L (${Fmt.number(l / 1000.0, 3)} m³)"
    }

    // ------------------------------------------------------------------ motor de maquetación

    private inner class Writer(private val doc: PdfDocument) {
        val pageW = 595
        val pageH = 842
        val margin = 36f
        val contentW = pageW - 2 * margin
        val bottomLimit = pageH - 48f

        private var pageNum = 0
        private var page: PdfDocument.Page? = null
        lateinit var canvas: Canvas
        var y = 0f

        private val primary = BrandingConfig.PRIMARY_COLOR
        private val accent = BrandingConfig.ACCENT_COLOR

        private val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9.5f; color = Color.rgb(20, 20, 20) }
        private val label = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f; color = Color.rgb(80, 80, 80); typeface = Typeface.DEFAULT_BOLD
        }
        private val small = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 7.5f; color = Color.rgb(100, 100, 100) }
        private val sectionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10.5f; color = Color.WHITE; typeface = Typeface.DEFAULT_BOLD
        }
        private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f; color = primary; typeface = Typeface.DEFAULT_BOLD
        }
        private val companyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f; color = primary; typeface = Typeface.DEFAULT_BOLD
        }
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(210, 215, 222); strokeWidth = 0.6f }

        init {
            newPage()
        }

        fun newPage() {
            page?.let { finishPage(it) }
            pageNum++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
            page = p
            canvas = p.canvas
            y = margin
            if (pageNum > 1) runningHeader()
        }

        fun finish() {
            page?.let { finishPage(it) }
            page = null
        }

        private fun finishPage(p: PdfDocument.Page) {
            val c = p.canvas
            c.drawLine(margin, pageH - 38f, pageW - margin, pageH - 38f, line)
            c.drawText(BrandingConfig.FOOTER_NOTE, margin, pageH - 26f, small)
            val right = "Página $pageNum · Generado ${Fmt.dateTime(System.currentTimeMillis())}"
            c.drawText(right, pageW - margin - small.measureText(right), pageH - 16f, small)
            doc.finishPage(p)
        }

        private fun runningHeader() {
            canvas.drawText(BrandingConfig.COMPANY_NAME + " · " + BrandingConfig.REPORT_TITLE, margin, y + 8f, small)
            canvas.drawLine(margin, y + 13f, pageW - margin, y + 13f, line)
            y += 22f
        }

        fun ensure(height: Float) {
            if (y + height > bottomLimit) newPage()
        }

        private fun layout(text: String, paint: TextPaint, width: Float): StaticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(10))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(1.5f, 1f)
                .setIncludePad(false)
                .build()

        private fun drawLayout(l: StaticLayout, x: Float, top: Float) {
            canvas.save()
            canvas.translate(x, top)
            l.draw(canvas)
            canvas.restore()
        }

        private fun loadLogo(name: String): Bitmap? {
            @Suppress("DiscouragedApi")
            val id = context.resources.getIdentifier(name, "drawable", context.packageName)
            if (id == 0) return null
            return try { BitmapFactory.decodeResource(context.resources, id) } catch (e: Exception) { null }
        }

        fun coverHeader() {
            ensure(120f)
            val logo = loadLogo(BrandingConfig.LOGO_RESOURCE_NAME)
            var textX = margin
            if (logo != null) {
                val h = 44f
                val wLogo = h * logo.width / logo.height
                canvas.drawBitmap(logo, null, RectF(margin, y, margin + wLogo, y + h), null)
                textX = margin + wLogo + 12f
                logo.recycle()
            }
            canvas.drawText(BrandingConfig.COMPANY_NAME, textX, y + 20f, companyPaint)
            canvas.drawText(BrandingConfig.REPORT_SUBTITLE, textX, y + 36f, small)
            val partner = loadLogo(BrandingConfig.PARTNER_LOGO_RESOURCE_NAME)
            if (partner != null) {
                val h = 36f
                val wLogo = h * partner.width / partner.height
                canvas.drawBitmap(partner, null, RectF(pageW - margin - wLogo, y, pageW - margin, y + h), null)
                partner.recycle()
            }
            y += 52f
            fill.color = primary
            canvas.drawRect(margin, y, pageW - margin, y + 2.5f, fill)
            y += 12f
            val tl = layout(BrandingConfig.REPORT_TITLE, titlePaint, contentW)
            drawLayout(tl, margin, y)
            y += tl.height + 10f
        }

        fun infoGrid(items: List<Pair<String, String>>) {
            val colW = contentW / 2
            val rowsCount = (items.size + 1) / 2
            val rowH = 30f
            ensure(rowsCount * rowH + 8f)
            fill.color = accent
            canvas.drawRect(margin, y, pageW - margin, y + rowsCount * rowH + 6f, fill)
            items.forEachIndexed { i, (k, v) ->
                val col = i % 2
                val row = i / 2
                val x = margin + 8f + col * colW
                val top = y + 6f + row * rowH
                canvas.drawText(k.uppercase(), x, top + 9f, small)
                val vPaint = TextPaint(body).apply { typeface = Typeface.DEFAULT_BOLD; textSize = 11f }
                val text = android.text.TextUtils.ellipsize(v, vPaint, colW - 16f, android.text.TextUtils.TruncateAt.END).toString()
                canvas.drawText(text, x, top + 23f, vPaint)
            }
            y += rowsCount * rowH + 14f
        }

        fun section(title: String) {
            ensure(46f)
            y += 4f
            fill.color = primary
            canvas.drawRect(margin, y, pageW - margin, y + 18f, fill)
            canvas.drawText(title, margin + 6f, y + 13f, sectionPaint)
            y += 24f
        }

        fun rows(vararg pairs: Pair<String, String>) = rows(pairs.toList())

        fun rows(pairs: List<Pair<String, String>>) {
            val labelW = 170f
            val valueW = contentW - labelW - 8f
            for ((k, vRaw) in pairs) {
                val v = vRaw.ifBlank { "—" }
                val lk = layout(k, label, labelW - 6f)
                val lv = layout(v, body, valueW)
                val h = maxOf(lk.height, lv.height) + 7f
                if (h > bottomLimit - margin - 30f) {
                    // Texto muy largo: se imprime como párrafo paginado.
                    ensure(lk.height + 6f)
                    drawLayout(lk, margin + 2f, y + 3f)
                    y += lk.height + 5f
                    paragraph(v)
                    continue
                }
                ensure(h)
                drawLayout(lk, margin + 2f, y + 3f)
                drawLayout(lv, margin + labelW, y + 3f)
                y += h
                canvas.drawLine(margin, y - 1f, pageW - margin, y - 1f, line)
            }
            y += 4f
        }

        fun keyData(items: List<Triple<String, String, Boolean>>) {
            val labelW = 170f
            val statusW = 62f
            val valueW = contentW - labelW - statusW - 12f
            val okPaint = TextPaint(label).apply { color = Color.rgb(27, 120, 50) }
            val missPaint = TextPaint(label).apply { color = Color.rgb(190, 60, 20) }
            for ((k, v, ok) in items) {
                val lk = layout(k, label, labelW - 6f)
                val vPaint = TextPaint(body).apply { typeface = Typeface.DEFAULT_BOLD; textSize = 10.5f }
                val lv = layout(v, vPaint, valueW)
                val h = maxOf(lk.height, lv.height) + 9f
                ensure(h)
                fill.color = accent
                canvas.drawRect(margin, y, pageW - margin, y + h - 2f, fill)
                drawLayout(lk, margin + 4f, y + 4f)
                drawLayout(lv, margin + labelW, y + 4f)
                val s = if (ok) "✓ Completo" else "Pendiente"
                canvas.drawText(s, pageW - margin - statusW, y + 14f, if (ok) okPaint else missPaint)
                y += h
            }
            y += 4f
        }

        fun note(text: String) {
            val l = layout(text, small, contentW)
            ensure(l.height + 6f)
            drawLayout(l, margin, y)
            y += l.height + 8f
        }

        /** Párrafo largo que puede partirse entre páginas (línea a línea). */
        fun paragraph(text: String) {
            val l = layout(text, body, contentW - 4f)
            var lineIdx = 0
            while (lineIdx < l.lineCount) {
                if (y + 14f > bottomLimit) newPage()
                val available = bottomLimit - y
                var end = lineIdx
                while (end < l.lineCount && (l.getLineBottom(end) - l.getLineTop(lineIdx)) <= available) end++
                if (end == lineIdx) end = lineIdx + 1
                val topPx = l.getLineTop(lineIdx).toFloat()
                val bottomPx = l.getLineBottom(end - 1).toFloat()
                canvas.save()
                canvas.translate(margin + 2f, y - topPx)
                canvas.clipRect(0f, topPx, contentW, bottomPx)
                l.draw(canvas)
                canvas.restore()
                y += bottomPx - topPx
                lineIdx = end
            }
            y += 8f
        }

        fun image(path: String, maxHeight: Float, label: String) {
            val bmp = ImageUtils.decodeSampled(path, 600) ?: return
            val h = maxHeight
            val w = (h * bmp.width / bmp.height).coerceAtMost(contentW)
            ensure(h + 16f)
            canvas.drawText(label, margin, y + 8f, small)
            val rect = RectF(margin, y + 12f, margin + w, y + 12f + h)
            val border = Paint(line).apply { style = Paint.Style.STROKE }
            canvas.drawBitmap(bmp, null, rect, null)
            canvas.drawRect(rect, border)
            bmp.recycle()
            y += h + 20f
        }

        fun photoGrid(photos: List<Pair<String, String>>) {
            val gap = 12f
            val cellW = (contentW - gap) / 2
            val imgH = 190f
            val cellH = imgH + 22f
            var col = 0
            for ((path, caption) in photos) {
                if (col == 0) ensure(cellH)
                val x = margin + col * (cellW + gap)
                val bmp = ImageUtils.decodeSampled(path, 900)
                if (bmp != null) {
                    val scale = minOf(cellW / bmp.width, imgH / bmp.height)
                    val dw = bmp.width * scale
                    val dh = bmp.height * scale
                    val left = x + (cellW - dw) / 2
                    val top = y + (imgH - dh) / 2
                    canvas.drawBitmap(bmp, null, RectF(left, top, left + dw, top + dh), null)
                    bmp.recycle()
                } else {
                    canvas.drawText("(imagen no disponible)", x + 8f, y + imgH / 2, small)
                }
                val border = Paint(line).apply { style = Paint.Style.STROKE }
                canvas.drawRect(x, y, x + cellW, y + imgH, border)
                val cap = android.text.TextUtils.ellipsize(caption, label, cellW, android.text.TextUtils.TruncateAt.END).toString()
                canvas.drawText(cap, x, y + imgH + 13f, label)
                col++
                if (col == 2) {
                    col = 0
                    y += cellH
                }
            }
            if (col != 0) y += cellH
        }
    }
}
