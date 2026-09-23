package com.jarabaimport.levantamiento.ui.tank

import android.os.Bundle
import android.widget.LinearLayout
import com.jarabaimport.levantamiento.domain.Fmt
import com.jarabaimport.levantamiento.domain.KeyData
import com.jarabaimport.levantamiento.domain.TankCalculations
import com.jarabaimport.levantamiento.domain.model.SurveyStatus
import com.jarabaimport.levantamiento.ui.BaseActivity
import com.jarabaimport.levantamiento.ui.Nav
import com.jarabaimport.levantamiento.ui.Palette
import com.jarabaimport.levantamiento.ui.addWithGap
import com.jarabaimport.levantamiento.ui.bigButton
import com.jarabaimport.levantamiento.ui.makeButton
import com.jarabaimport.levantamiento.ui.makeStatusChip
import com.jarabaimport.levantamiento.ui.makeText
import com.jarabaimport.levantamiento.ui.makeTile
import com.jarabaimport.levantamiento.ui.photos.photoCategories
import com.jarabaimport.levantamiento.ui.row
import com.jarabaimport.levantamiento.ui.sectionCard
import com.jarabaimport.levantamiento.ui.sectionTitle
import com.jarabaimport.levantamiento.ui.text
import com.jarabaimport.levantamiento.ui.weighted

/** RESUMEN DEL LEVANTAMIENTO: datos clave muy visibles + acciones (editar, duplicar, PDF, CSV, estado). */
class TankSummaryActivity : BaseActivity() {

    private var tankId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tankId = intent.getLongExtra(Nav.EXTRA_TANK, 0L)
        setupScreen("Resumen del levantamiento")
        addHeaderAction("BORRAR") { confirmDelete() }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        tankId = intent.getLongExtra(Nav.EXTRA_TANK, tankId)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val t = c.tanks.get(tankId) ?: run { finish(); return }
        val client = c.clients.get(t.clientId)
        val photos = c.photos.byTank(tankId)
        setTitles("Resumen del levantamiento", client?.name)
        val refH = TankCalculations.referenceHeightMm(t.cylindricalHeightMm, t.totalHeightMm)
        rebuild {
            row(14) {
                weighted(makeText(context, "${KeyData.completedCount(t)} / 7 datos principales", 17f, bold = true))
                addView(makeStatusChip(context, t.status))
            }
            fun tiles(vararg items: LinearLayout) = row(10) { items.forEach { weighted(it) } }
            tiles(
                makeTile(this@TankSummaryActivity, "Cliente", client?.name ?: "—") { Nav.client(this@TankSummaryActivity, t.clientId) },
                makeTile(this@TankSummaryActivity, "Tanque", t.code.ifBlank { t.name }),
            )
            tiles(
                makeTile(this@TankSummaryActivity, "Producto", t.product, key = true),
                makeTile(this@TankSummaryActivity, "Volumen", Fmt.withUnit(t.nominalVolumeL, "L", 0)),
            )
            tiles(
                makeTile(this@TankSummaryActivity, "Diámetro", Fmt.withUnit(t.internalDiameterMm, "mm", 0), key = true),
                makeTile(this@TankSummaryActivity, "Altura", Fmt.withUnit(refH, "mm", 0), key = true),
            )
            tiles(
                makeTile(this@TankSummaryActivity, "Presión CIP", Fmt.withUnit(t.cipPressureBar, "bar"), key = true),
                makeTile(this@TankSummaryActivity, "Caudal CIP", Fmt.withUnit(t.cipFlowM3h, "m³/h"), key = true),
            )
            addWithGap(makeTile(this@TankSummaryActivity, "Residuo", KeyData.residueText(t), key = true), 10)
            addWithGap(makeTile(this@TankSummaryActivity, "Obstáculos", KeyData.obstaclesText(t), key = true), 10)
            addWithGap(makeTile(this@TankSummaryActivity, "Cabezal actual", when (t.hasExistingHead) {
                true -> t.currentHeadModel.ifBlank { "Sí (modelo no indicado)" }
                false -> "No disponible"
                null -> "—"
            }), 14)

            val est = TankCalculations.cylindricalVolumeLiters(t.internalDiameterMm, t.cylindricalHeightMm)
            val hd = TankCalculations.heightToDiameterRatio(refH, t.internalDiameterMm)
            text("H/D: ${hd?.let { Fmt.number(it, 2) } ?: "—"}  ·  Volumen cilíndrico estimado: ${est?.let { Fmt.withUnit(it, "L", 0) } ?: "—"} (estimación)",
                14f, color = Palette.MUTED, gapDp = 4)
            text("Área: ${t.area.ifBlank { "—" }}  ·  Proceso: ${t.process.ifBlank { "—" }}  ·  Fecha: ${Fmt.date(t.surveyDate)}  ·  Técnico: ${t.technician.ifBlank { "—" }}",
                14f, color = Palette.MUTED, gapDp = 16)

            bigButton("✎  Editar levantamiento") { Nav.wizard(this@TankSummaryActivity, t.clientId, t.id, 0) }
            bigButton("Duplicar tanque", primary = false) { confirmDuplicate() }
            row {
                weighted(makeButton(context, "Exportar PDF", primary = false) { runExport { c.exports.tankPdf(tankId) } })
                weighted(makeButton(context, "Exportar CSV", primary = false) { runExport { c.exports.tankCsv(tankId) } })
            }
            bigButton("Cambiar estado", primary = false) { statusDialog() }

            sectionTitle("Fotografías (${photos.size})")
            if (photos.isEmpty()) text("Sin fotografías. Use \"Editar levantamiento\" → paso 7.", 15f, color = Palette.MUTED, gapDp = 14)
            else photoCategories(this, photos, editable = false)

            if (t.observations.isNotBlank()) sectionCard("Observaciones") { text(t.observations, 16f, gapDp = 0) }
        }
    }

    private fun confirmDuplicate() {
        confirm("Duplicar tanque",
            "Se creará una copia con todos los datos técnicos (sin fotos ni firma) y el siguiente código. Después solo modifique las diferencias.",
            "Duplicar") {
            val t = c.tanks.get(tankId) ?: return@confirm
            val newId = c.tanks.duplicate(tankId) ?: return@confirm
            toast("Tanque duplicado.")
            Nav.wizard(this, t.clientId, newId, 0)
        }
    }

    private fun statusDialog() {
        choiceDialog("Estado del levantamiento",
            "Borrador / Levantamiento completado se asignan automáticamente según los 7 datos principales.",
            listOf(
                "Automático (Borrador / Completado)" to { c.tanks.resetAutoStatus(tankId); render() },
                SurveyStatus.PENDING_REVIEW.label to { c.tanks.setStatus(tankId, SurveyStatus.PENDING_REVIEW.code); render() },
                SurveyStatus.REVIEWED.label to { c.tanks.setStatus(tankId, SurveyStatus.REVIEWED.code); render() },
            ), primaryFirst = false)
    }

    private fun confirmDelete() {
        confirm("Borrar tanque", "Se borrarán el levantamiento y sus fotografías. No se puede deshacer.", "Borrar") {
            c.tanks.delete(tankId)
            if (c.settings.pendingTankId == tankId) c.settings.clearPending()
            toast("Tanque borrado.")
            finish()
        }
    }
}
