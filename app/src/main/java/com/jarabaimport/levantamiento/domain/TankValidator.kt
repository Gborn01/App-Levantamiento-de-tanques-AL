package com.jarabaimport.levantamiento.domain

import com.jarabaimport.levantamiento.data.db.TankEntity

/**
 * Validación de datos del tanque. Devuelve un mapa campo → mensaje.
 * Nombres de campo = nombres de propiedad de [TankEntity].
 */
object TankValidator {

    const val MSG_NAME = "El nombre del tanque es obligatorio."
    const val MSG_PRODUCT = "El producto procesado es obligatorio."
    const val MSG_POSITIVE = "Debe ser mayor que 0."
    const val MSG_NON_NEGATIVE = "No puede ser negativo."

    /** Errores por paso del asistente (índice 0..7). */
    fun errorsForStep(tank: TankEntity, step: Int): Map<String, String> = when (step) {
        0 -> identification(tank)
        1 -> product(tank)
        2 -> geometry(tank)
        3 -> cip(tank)
        4 -> internals(tank)
        5 -> head(tank)
        else -> emptyMap()
    }

    fun validateAll(tank: TankEntity): Map<String, String> =
        (0..7).fold(emptyMap<String, String>()) { acc, s -> acc + errorsForStep(tank, s) }

    /**
     * Para el AUTOGUARDADO: devuelve una copia donde los valores numéricos inválidos
     * se guardan como "no informado" (null). Así nunca se persisten datos evidentemente inválidos.
     */
    fun sanitizeForStorage(t: TankEntity): TankEntity {
        val bad = validateAll(t).keys
        if (bad.isEmpty()) return t
        var r = t
        for (k in bad) {
            r = when (k) {
                "internalDiameterMm" -> r.copy(internalDiameterMm = null)
                "cylindricalHeightMm" -> r.copy(cylindricalHeightMm = null)
                "totalHeightMm" -> r.copy(totalHeightMm = null)
                "nominalVolumeL" -> r.copy(nominalVolumeL = null)
                "workingVolumeL" -> r.copy(workingVolumeL = null)
                "bottomAngleDeg" -> r.copy(bottomAngleDeg = null)
                "cipFlowM3h" -> r.copy(cipFlowM3h = null)
                "cipPressureBar" -> r.copy(cipPressureBar = null)
                "cipTimeMin" -> r.copy(cipTimeMin = null)
                "cipPipeLengthM" -> r.copy(cipPipeLengthM = null)
                "cipPipeDiameterIn" -> r.copy(cipPipeDiameterIn = null)
                "naohConcentrationPct" -> r.copy(naohConcentrationPct = null)
                "acidConcentrationPct" -> r.copy(acidConcentrationPct = null)
                "cipTemperatureC" -> r.copy(cipTemperatureC = null)
                "agitatorDiameterMm" -> r.copy(agitatorDiameterMm = null)
                "agitatorHeightMm" -> r.copy(agitatorHeightMm = null)
                "coilDiameterMm" -> r.copy(coilDiameterMm = null)
                "bafflesCount" -> r.copy(bafflesCount = null)
                "headHeightAboveBottomMm" -> r.copy(headHeightAboveBottomMm = null)
                else -> r // campos de texto obligatorios: se mantienen como borrador
            }
        }
        return r
    }

    /** Primer paso (0..7) que contiene errores, o null. */
    fun firstStepWithErrors(t: TankEntity): Int? = (0..7).firstOrNull { errorsForStep(t, it).isNotEmpty() }

    private fun identification(t: TankEntity) = buildMap<String, String> {
        if (t.name.isBlank()) put("name", MSG_NAME)
    }

    private fun product(t: TankEntity) = buildMap<String, String> {
        if (t.product.isBlank()) put("product", MSG_PRODUCT)
    }

    private fun geometry(t: TankEntity) = buildMap<String, String> {
        positive(t.internalDiameterMm, "internalDiameterMm")
        positive(t.cylindricalHeightMm, "cylindricalHeightMm")
        positive(t.totalHeightMm, "totalHeightMm")
        nonNegative(t.nominalVolumeL, "nominalVolumeL")
        nonNegative(t.workingVolumeL, "workingVolumeL")
        val ang = t.bottomAngleDeg
        if (ang != null && (ang < 0 || ang > 90)) put("bottomAngleDeg", "El ángulo debe estar entre 0° y 90°.")
        val cyl = t.cylindricalHeightMm
        val tot = t.totalHeightMm
        if (cyl != null && tot != null && cyl > 0 && tot > 0 && tot < cyl) {
            put("totalHeightMm", "La altura total no puede ser menor que la altura cilíndrica.")
        }
        val nom = t.nominalVolumeL
        val work = t.workingVolumeL
        if (nom != null && work != null && nom > 0 && work > nom) {
            put("workingVolumeL", "El volumen de trabajo no puede superar el nominal.")
        }
    }

    private fun cip(t: TankEntity) = buildMap<String, String> {
        nonNegative(t.cipFlowM3h, "cipFlowM3h")
        nonNegative(t.cipPressureBar, "cipPressureBar")
        nonNegative(t.cipTimeMin, "cipTimeMin")
        nonNegative(t.cipPipeLengthM, "cipPipeLengthM")
        positive(t.cipPipeDiameterIn, "cipPipeDiameterIn")
        percent(t.naohConcentrationPct, "naohConcentrationPct")
        percent(t.acidConcentrationPct, "acidConcentrationPct")
        val temp = t.cipTemperatureC
        if (temp != null && (temp < 0 || temp > 150)) put("cipTemperatureC", "Temperatura fuera de rango (0–150 °C).")
    }

    private fun internals(t: TankEntity) = buildMap<String, String> {
        if (t.hasAgitator) {
            positive(t.agitatorDiameterMm, "agitatorDiameterMm")
            nonNegative(t.agitatorHeightMm, "agitatorHeightMm")
        }
        if (t.hasCoil) positive(t.coilDiameterMm, "coilDiameterMm")
        val n = t.bafflesCount
        if (t.hasBaffles && n != null && n <= 0) put("bafflesCount", MSG_POSITIVE)
        if (t.hasOtherObstacles && t.otherObstacles.isBlank()) put("otherObstacles", "Describa el obstáculo.")
    }

    private fun head(t: TankEntity) = buildMap<String, String> {
        nonNegative(t.headHeightAboveBottomMm, "headHeightAboveBottomMm")
    }

    private fun MutableMap<String, String>.positive(v: Double?, key: String) {
        if (v != null && v <= 0) put(key, MSG_POSITIVE)
    }

    private fun MutableMap<String, String>.nonNegative(v: Double?, key: String) {
        if (v != null && v < 0) put(key, MSG_NON_NEGATIVE)
    }

    private fun MutableMap<String, String>.percent(v: Double?, key: String) {
        if (v != null && (v < 0 || v > 100)) put(key, "Debe estar entre 0 y 100 %.")
    }
}
