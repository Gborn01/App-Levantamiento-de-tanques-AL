package com.jarabaimport.levantamiento.ui.tank

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.widget.LinearLayout
import com.jarabaimport.levantamiento.data.db.TankEntity
import com.jarabaimport.levantamiento.domain.Fmt
import com.jarabaimport.levantamiento.domain.KeyData
import com.jarabaimport.levantamiento.domain.TankCalculations
import com.jarabaimport.levantamiento.domain.model.FieldOptions
import com.jarabaimport.levantamiento.domain.model.HeadPosition
import com.jarabaimport.levantamiento.domain.model.PressureSource
import com.jarabaimport.levantamiento.domain.model.ResidueCharacteristic
import com.jarabaimport.levantamiento.ui.Field
import com.jarabaimport.levantamiento.ui.Palette
import com.jarabaimport.levantamiento.ui.bigButton
import com.jarabaimport.levantamiento.ui.card
import com.jarabaimport.levantamiento.ui.checkRow
import com.jarabaimport.levantamiento.ui.dp
import com.jarabaimport.levantamiento.ui.labeledValue
import com.jarabaimport.levantamiento.ui.makeButton
import com.jarabaimport.levantamiento.ui.numberField
import com.jarabaimport.levantamiento.ui.photos.photoCategories
import com.jarabaimport.levantamiento.ui.radioList
import com.jarabaimport.levantamiento.ui.row
import com.jarabaimport.levantamiento.ui.sectionCard
import com.jarabaimport.levantamiento.ui.suggestField
import com.jarabaimport.levantamiento.ui.text
import com.jarabaimport.levantamiento.ui.textField
import com.jarabaimport.levantamiento.ui.weighted
import java.util.Calendar

/** Pasos del asistente. */
val WIZARD_STEPS = listOf(
    "IDENTIFICACIÓN",
    "PRODUCTO",
    "GEOMETRÍA",
    "CIP",
    "INTERNOS",
    "CABEZAL",
    "FOTOGRAFÍAS",
    "OBSERVACIONES Y FIRMA",
)

/**
 * Contexto para construir un paso.
 * - [u]: cambia el borrador (texto; no redibuja).
 * - [ur]: cambia el borrador y redibuja el paso (para mostrar/ocultar secciones).
 * - [reg]: registra un campo para mostrarle errores de validación.
 */
class StepEnv(val act: TankWizardActivity, val t: TankEntity, val errors: Map<String, String>) {
    fun u(f: (TankEntity) -> TankEntity) = act.update(f, rerender = false)
    fun ur(f: (TankEntity) -> TankEntity) = act.update(f, rerender = true)
    fun reg(key: String, field: Field): Field {
        act.fields[key] = field
        field.setError(errors[key])
        return field
    }
}

fun LinearLayout.buildStep(step: Int, e: StepEnv) = when (step) {
    0 -> stepIdentification(e)
    1 -> stepProduct(e)
    2 -> stepGeometry(e)
    3 -> stepCip(e)
    4 -> stepInternals(e)
    5 -> stepHead(e)
    6 -> stepPhotos(e)
    else -> stepObservations(e)
}

// ---------------------------------------------------------------- Paso 1
private fun LinearLayout.stepIdentification(e: StepEnv) {
    val t = e.t
    sectionCard("Identificación") {
        text("Cliente: ${e.act.clientName}", 17f, bold = true, gapDp = 12)
        e.reg("name", textField("Nombre del tanque", t.name, required = true, hint = "Ej.: Tanque de mayonesa") { v -> e.u { it.copy(name = v) } })
        textField("Código", t.code, hint = "Ej.: TK-001") { v -> e.u { it.copy(code = v) } }
        textField("Área / planta", t.area) { v -> e.u { it.copy(area = v) } }
        suggestField("Proceso", t.process, FieldOptions.processes) { v -> e.u { it.copy(process = v) } }
    }
}

// ---------------------------------------------------------------- Paso 2
private fun LinearLayout.stepProduct(e: StepEnv) {
    val t = e.t
    sectionCard("Producto y residuo", highlight = true) {
        e.reg("product", textField("Producto procesado", t.product, required = true, isKey = true, hint = "Ej.: Mayonesa") { v ->
            e.u { it.copy(product = v) }
        })
        textField("Descripción del producto", t.productDescription) { v -> e.u { it.copy(productDescription = v) } }
        textField(
            "Residuo después del vaciado", t.residue, multiline = true, isKey = true,
            hint = "Ej.: Película grasa/viscosa adherida a paredes, fondo y agitador.",
            help = "Describa el residuo real; no es lo mismo que el producto."
        ) { v -> e.u { it.copy(residue = v) } }
    }
    sectionCard("Características del residuo") {
        text("Seleccione una o varias:", 15f, color = Palette.MUTED)
        val selected = ResidueCharacteristic.decode(t.residueCharacteristics)
        ResidueCharacteristic.entries.forEach { rc ->
            checkRow(rc.label, rc in selected) { checked ->
                val now = ResidueCharacteristic.decode(e.act.draft.residueCharacteristics)
                val set = if (checked) now + rc else now - rc
                if (rc == ResidueCharacteristic.OTHER) e.ur { it.copy(residueCharacteristics = ResidueCharacteristic.encode(set)) }
                else e.u { it.copy(residueCharacteristics = ResidueCharacteristic.encode(set)) }
            }
        }
        if (ResidueCharacteristic.OTHER in selected) {
            textField("Especifique (otro)", t.residueOther) { v -> e.u { it.copy(residueOther = v) } }
        }
    }
    sectionCard("Observaciones") {
        textField("Observaciones de producto / residuo", t.productObservations, multiline = true) { v -> e.u { it.copy(productObservations = v) } }
    }
}

// ---------------------------------------------------------------- Paso 3
private fun LinearLayout.stepGeometry(e: StepEnv) {
    val t = e.t
    lateinit var calc: LinearLayout
    fun refreshCalc() {
        val d = e.act.draft
        calc.removeAllViews()
        val est = TankCalculations.cylindricalVolumeLiters(d.internalDiameterMm, d.cylindricalHeightMm)
        val refH = TankCalculations.referenceHeightMm(d.cylindricalHeightMm, d.totalHeightMm)
        val hd = TankCalculations.heightToDiameterRatio(refH, d.internalDiameterMm)
        calc.text("CÁLCULOS AUTOMÁTICOS", 16f, bold = true, color = Palette.NAVY, gapDp = 8)
        calc.text("Volumen cilíndrico estimado", 14f, bold = true, color = Palette.MUTED, gapDp = 2)
        calc.text(if (est == null) "Introduzca diámetro y altura cilíndrica." else "${Fmt.number(est, 0)} L  (${Fmt.number(est / 1000, 3)} m³)",
            20f, bold = true, gapDp = 4)
        calc.text("⚠ Estimación: V = π × D² / 4 × H. NO sustituye el volumen nominal del fabricante.", 14f, color = 0xFF8A5A00.toInt(), gapDp = 10)
        calc.text("Relación altura / diámetro (H/D)", 14f, bold = true, color = Palette.MUTED, gapDp = 2)
        calc.text(hd?.let { Fmt.number(it, 2) } ?: "—", 20f, bold = true, gapDp = 0)
    }
    fun uc(f: (TankEntity) -> TankEntity) { e.u(f); refreshCalc() }

    sectionCard("Geometría del tanque", highlight = true) {
        e.reg("internalDiameterMm", numberField("Diámetro interno", "mm", t.internalDiameterMm, isKey = true) { v -> uc { it.copy(internalDiameterMm = v) } })
        e.reg("cylindricalHeightMm", numberField("Altura cilíndrica", "mm", t.cylindricalHeightMm, isKey = true) { v -> uc { it.copy(cylindricalHeightMm = v) } })
        e.reg("totalHeightMm", numberField("Altura total", "mm", t.totalHeightMm) { v -> uc { it.copy(totalHeightMm = v) } })

        text("Unidad de volumen", 15f, bold = true, color = Palette.MUTED, gapDp = 4)
        val m3 = e.act.volumeInM3
        row(12) {
            weighted(makeButton(context, "Litros (L)", primary = !m3) { e.act.volumeInM3 = false; e.act.renderStep() })
            weighted(makeButton(context, "m³", primary = m3) { e.act.volumeInM3 = true; e.act.renderStep() })
        }
        val unit = if (m3) "m³" else "L"
        fun toUi(l: Double?) = l?.let { if (m3) TankCalculations.litersToM3(it) else it }
        fun fromUi(x: Double?) = x?.let { if (m3) TankCalculations.m3ToLiters(it) else it }
        e.reg("nominalVolumeL", numberField("Volumen nominal", unit, toUi(t.nominalVolumeL)) { v -> e.u { it.copy(nominalVolumeL = fromUi(v)) } })
        e.reg("workingVolumeL", numberField("Volumen de trabajo", unit, toUi(t.workingVolumeL)) { v -> e.u { it.copy(workingVolumeL = fromUi(v)) } })

        suggestField("Tipo de techo", t.roofType, FieldOptions.roofTypes) { v -> e.u { it.copy(roofType = v) } }
        suggestField("Tipo de fondo", t.bottomType, FieldOptions.bottomTypes) { v -> e.u { it.copy(bottomType = v) } }
        e.reg("bottomAngleDeg", numberField("Ángulo del fondo", "°", t.bottomAngleDeg) { v -> e.u { it.copy(bottomAngleDeg = v) } })
        suggestField("Material", t.material, FieldOptions.materials) { v -> e.u { it.copy(material = v) } }
        suggestField("Acabado interno", t.internalFinish, FieldOptions.finishes) { v -> e.u { it.copy(internalFinish = v) } }
    }
    calc = card { }
    refreshCalc()
}

// ---------------------------------------------------------------- Paso 4
private fun LinearLayout.stepCip(e: StepEnv) {
    val t = e.t
    sectionCard("Parámetros CIP", highlight = true) {
        e.reg("cipFlowM3h", numberField("Caudal disponible", "m³/h", t.cipFlowM3h, isKey = true) { v -> e.u { it.copy(cipFlowM3h = v) } })
        e.reg("cipPressureBar", numberField(
            "Presión disponible en el cabezal", "bar", t.cipPressureBar, isKey = true,
            help = "Preferiblemente la presión MEDIDA durante el CIP en la entrada del cabezal, no la nominal de la bomba."
        ) { v -> e.u { it.copy(cipPressureBar = v) } })
        text("¿De dónde proviene el dato de presión?", 15f, bold = true, color = Palette.MUTED, gapDp = 4)
        radioList(PressureSource.entries.toList(), PressureSource.fromCode(t.cipPressureSource), { it.label }) { p ->
            e.u { it.copy(cipPressureSource = p?.code ?: "") }
        }
        e.reg("cipTemperatureC", numberField("Temperatura", "°C", t.cipTemperatureC) { v -> e.u { it.copy(cipTemperatureC = v) } })
        e.reg("naohConcentrationPct", numberField("Concentración NaOH", "%", t.naohConcentrationPct) { v -> e.u { it.copy(naohConcentrationPct = v) } })
        e.reg("acidConcentrationPct", numberField("Concentración ácido", "%", t.acidConcentrationPct) { v -> e.u { it.copy(acidConcentrationPct = v) } })
        e.reg("cipTimeMin", numberField("Tiempo total CIP", "min", t.cipTimeMin) { v -> e.u { it.copy(cipTimeMin = v) } })
        e.reg("cipPipeDiameterIn", numberField("Diámetro tubería CIP", "pulg", t.cipPipeDiameterIn) { v -> e.u { it.copy(cipPipeDiameterIn = v) } })
        e.reg("cipPipeLengthM", numberField("Longitud aproximada", "m", t.cipPipeLengthM) { v -> e.u { it.copy(cipPipeLengthM = v) } })
    }
    sectionCard("Otros elementos en la línea") {
        checkRow("Filtros", t.cipHasFilters) { v -> e.u { it.copy(cipHasFilters = v) } }
        checkRow("Válvulas", t.cipHasValves) { v -> e.u { it.copy(cipHasValves = v) } }
        checkRow("Codos", t.cipHasElbows) { v -> e.u { it.copy(cipHasElbows = v) } }
        checkRow("Restricciones", t.cipHasRestrictions) { v -> e.u { it.copy(cipHasRestrictions = v) } }
        textField("Detalle / otros elementos", t.cipOtherElements, multiline = true) { v -> e.u { it.copy(cipOtherElements = v) } }
    }
}

// ---------------------------------------------------------------- Paso 5
private fun LinearLayout.stepInternals(e: StepEnv) {
    val t = e.t
    sectionCard("Internos del tanque", highlight = true) {
        text("★ Dato principal: obstáculos internos. Marque todos los que existan.", 15f, color = 0xFF7A5500.toInt(), gapDp = 8)
        checkRow("Sin obstáculos internos", t.noObstacles) { v ->
            e.ur {
                if (v) it.copy(noObstacles = true, hasAgitator = false, hasCoil = false, hasBaffles = false, hasInternalTubes = false,
                    hasSensors = false, hasInternalPiping = false, hasOtherObstacles = false)
                else it.copy(noObstacles = false)
            }
        }
        fun obstacle(label: String, value: Boolean, set: (TankEntity, Boolean) -> TankEntity) =
            checkRow(label, value) { v -> e.ur { set(it, v).copy(noObstacles = if (v) false else it.noObstacles) } }
        obstacle("Agitador", t.hasAgitator) { x, v -> x.copy(hasAgitator = v) }
        obstacle("Serpentín", t.hasCoil) { x, v -> x.copy(hasCoil = v) }
        obstacle("Baffles", t.hasBaffles) { x, v -> x.copy(hasBaffles = v) }
        obstacle("Tubos internos", t.hasInternalTubes) { x, v -> x.copy(hasInternalTubes = v) }
        obstacle("Sensores", t.hasSensors) { x, v -> x.copy(hasSensors = v) }
        obstacle("Tuberías internas", t.hasInternalPiping) { x, v -> x.copy(hasInternalPiping = v) }
        obstacle("Otros", t.hasOtherObstacles) { x, v -> x.copy(hasOtherObstacles = v) }
    }
    if (t.hasAgitator) sectionCard("Agitador") {
        e.reg("agitatorDiameterMm", numberField("Diámetro del agitador", "mm", t.agitatorDiameterMm) { v -> e.u { it.copy(agitatorDiameterMm = v) } })
        e.reg("agitatorHeightMm", numberField("Altura del agitador (sobre el fondo)", "mm", t.agitatorHeightMm) { v -> e.u { it.copy(agitatorHeightMm = v) } })
        textField("Descripción del agitador", t.agitatorDescription, multiline = true,
            hint = "Ej.: Agitador central de 1,200 mm de diámetro con 4 paletas.") { v -> e.u { it.copy(agitatorDescription = v) } }
    }
    if (t.hasCoil) sectionCard("Serpentín") {
        suggestField("Tipo de serpentín", t.coilType, FieldOptions.coilTypes) { v -> e.u { it.copy(coilType = v) } }
        e.reg("coilDiameterMm", numberField("Diámetro del serpentín", "mm", t.coilDiameterMm) { v -> e.u { it.copy(coilDiameterMm = v) } })
        textField("Dimensiones", t.coilDimensions) { v -> e.u { it.copy(coilDimensions = v) } }
        textField("Descripción del serpentín", t.coilDescription, multiline = true) { v -> e.u { it.copy(coilDescription = v) } }
    }
    if (t.hasBaffles) sectionCard("Baffles") {
        e.reg("bafflesCount", numberField("Cantidad de baffles", "ud", t.bafflesCount?.toDouble(), integer = true) { v ->
            e.u { it.copy(bafflesCount = v?.toInt()) }
        })
        textField("Dimensiones aproximadas", t.bafflesDimensions) { v -> e.u { it.copy(bafflesDimensions = v) } }
    }
    if (t.hasOtherObstacles) sectionCard("Otros obstáculos") {
        e.reg("otherObstacles", textField("Describa el obstáculo", t.otherObstacles, multiline = true) { v -> e.u { it.copy(otherObstacles = v) } })
    }
    if (t.hasInternalTubes || t.hasSensors || t.hasInternalPiping) sectionCard("Tubos / sensores / tuberías") {
        text("Descríbalos en \"Otros\" o en las observaciones finales y fotografíelos en el paso 7.", 15f, color = Palette.MUTED)
    }
}

// ---------------------------------------------------------------- Paso 6
private fun LinearLayout.stepHead(e: StepEnv) {
    val t = e.t
    sectionCard("Instalación del cabezal") {
        text("¿Existe cabezal actualmente?", 15f, bold = true, color = Palette.MUTED, gapDp = 4)
        radioList(listOf(true, false), t.hasExistingHead, { if (it) "Sí" else "No" }) { v -> e.ur { it.copy(hasExistingHead = v) } }
        if (t.hasExistingHead == true) textField("Modelo actual", t.currentHeadModel) { v -> e.u { it.copy(currentHeadModel = v) } }
        textField("Ubicación", t.headLocation, hint = "Ej.: Boquilla superior central del techo") { v -> e.u { it.copy(headLocation = v) } }
        e.reg("headHeightAboveBottomMm", numberField("Altura sobre el fondo", "mm", t.headHeightAboveBottomMm) { v ->
            e.u { it.copy(headHeightAboveBottomMm = v) }
        })
        suggestField("Tipo de conexión", t.headConnectionType, FieldOptions.connectionTypes) { v -> e.u { it.copy(headConnectionType = v) } }
        textField("Diámetro de conexión", t.headConnectionDiameter, hint = "Ej.: 1½\" / DN40") { v -> e.u { it.copy(headConnectionDiameter = v) } }
        text("Posición", 15f, bold = true, color = Palette.MUTED, gapDp = 4)
        radioList(HeadPosition.entries.toList(), HeadPosition.fromCode(t.headPosition), { it.label }) { p ->
            e.ur { it.copy(headPosition = p?.code ?: "") }
        }
        if (t.headPosition == HeadPosition.OTHER.code) textField("Especifique la posición", t.headPositionOther) { v -> e.u { it.copy(headPositionOther = v) } }
        text("Esta versión solo registra datos; no selecciona modelos de cabezal.", 14f, color = Palette.MUTED, gapDp = 0)
    }
}

// ---------------------------------------------------------------- Paso 7
private fun LinearLayout.stepPhotos(e: StepEnv) {
    card {
        text("Las fotos se guardan en el teléfono y quedan asociadas a este tanque. Toque una miniatura para verla o borrarla.", 15f, gapDp = 0)
    }
    e.act.photoCategories(this, e.act.photos, editable = true,
        onTake = { e.act.takePhoto(it) }, onPick = { e.act.pickPhoto(it) }, onDelete = { e.act.deletePhoto(it) })
}

// ---------------------------------------------------------------- Paso 8
private fun LinearLayout.stepObservations(e: StepEnv) {
    val t = e.t
    val act = e.act
    sectionCard("Observaciones") {
        textField("Observaciones generales", t.observations, multiline = true) { v -> e.u { it.copy(observations = v) } }
    }
    sectionCard("Responsables") {
        textField("Técnico responsable", t.technician) { v -> e.u { it.copy(technician = v) } }
        textField("Representante del cliente", t.clientRepresentative) { v -> e.u { it.copy(clientRepresentative = v) } }
        labeledValue("Fecha del levantamiento", Fmt.date(t.surveyDate))
        bigButton("Cambiar fecha", primary = false) {
            val cal = Calendar.getInstance().apply { timeInMillis = act.draft.surveyDate }
            DatePickerDialog(act, { _, y, m, d ->
                val c2 = Calendar.getInstance().apply { timeInMillis = act.draft.surveyDate; set(y, m, d) }
                e.ur { it.copy(surveyDate = c2.timeInMillis) }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
    }
    sectionCard("Firma") {
        val pad = SignatureView(context)
        if (t.signaturePath.isNotBlank()) pad.existing = BitmapFactory.decodeFile(t.signaturePath)
        text(if (t.signaturePath.isNotBlank()) "Firma guardada. Puede firmar de nuevo para reemplazarla." else "Firme en el recuadro con el dedo.",
            14f, color = Palette.MUTED, gapDp = 6)
        addView(pad, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, act.dp(200)).apply { bottomMargin = act.dp(10) })
        pad.background = com.jarabaimport.levantamiento.ui.rounded(0xFFFFFFFF.toInt(), 0f, Palette.OUTLINE, act.dp(2))
        row(4) {
            weighted(makeButton(context, "Guardar firma") {
                if (!pad.hasDrawing) act.toast("Dibuje la firma primero.") else act.saveSignature(pad.toBitmap())
            })
            weighted(makeButton(context, "Borrar firma", primary = false) { pad.clear(); act.clearSignature() })
        }
    }
    val items = KeyData.items(t)
    val done = items.count { it.complete }
    sectionCard("Datos principales: $done / 7", highlight = true) {
        items.forEach { item ->
            text((if (item.complete) "✓ " else "✗ ") + "${item.number}. ${item.label}: ${item.value}", 15f,
                color = if (item.complete) Palette.TEXT else Palette.ERROR, gapDp = 4)
        }
        text(
            if (done == 7) "Al finalizar, el estado será \"Levantamiento completado\"."
            else "Puede finalizar igualmente; quedará como \"Borrador\" hasta completar los 7 datos.",
            14f, bold = true, gapDp = 0
        )
    }
}
