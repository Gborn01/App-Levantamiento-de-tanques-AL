package com.jarabaimport.levantamiento.domain

import com.jarabaimport.levantamiento.data.db.TankEntity
import com.jarabaimport.levantamiento.domain.model.ResidueCharacteristic
import com.jarabaimport.levantamiento.domain.model.SurveyStatus

/**
 * Los SIETE DATOS PRINCIPALES para una primera evaluación de selección de cabezal.
 * Solo evalúa si están completos: NO hace diagnósticos ni recomendaciones.
 */
data class KeyDataItem(val number: Int, val label: String, val value: String, val complete: Boolean)

object KeyData {

    fun obstaclesDeclared(t: TankEntity): Boolean =
        t.noObstacles || t.hasAgitator || t.hasCoil || t.hasBaffles || t.hasInternalTubes ||
            t.hasSensors || t.hasInternalPiping || t.hasOtherObstacles

    fun obstaclesText(t: TankEntity): String {
        if (t.noObstacles) return "Sin obstáculos internos"
        val list = buildList {
            if (t.hasAgitator) add("Agitador")
            if (t.hasCoil) add("Serpentín")
            if (t.hasBaffles) add("Baffles")
            if (t.hasInternalTubes) add("Tubos internos")
            if (t.hasSensors) add("Sensores")
            if (t.hasInternalPiping) add("Tuberías internas")
            if (t.hasOtherObstacles) add("Otros")
        }
        return if (list.isEmpty()) "—" else list.joinToString(" + ")
    }

    fun residueText(t: TankEntity): String {
        val chars = ResidueCharacteristic.decode(t.residueCharacteristics).joinToString(", ") { it.label }
        return when {
            t.residue.isNotBlank() && chars.isNotEmpty() -> "${t.residue} ($chars)"
            t.residue.isNotBlank() -> t.residue
            chars.isNotEmpty() -> chars
            else -> "—"
        }
    }

    fun items(t: TankEntity): List<KeyDataItem> {
        val height = TankCalculations.referenceHeightMm(t.cylindricalHeightMm, t.totalHeightMm)
        return listOf(
            KeyDataItem(1, "Producto procesado", t.product.ifBlank { "—" }, t.product.isNotBlank()),
            KeyDataItem(
                2, "Residuo / característica", residueText(t),
                t.residue.isNotBlank() || t.residueCharacteristics.isNotBlank()
            ),
            KeyDataItem(3, "Diámetro interno", Fmt.withUnit(t.internalDiameterMm, "mm", 0), (t.internalDiameterMm ?: 0.0) > 0),
            KeyDataItem(4, "Altura del tanque", Fmt.withUnit(height, "mm", 0), height != null),
            KeyDataItem(5, "Presión CIP disponible", Fmt.withUnit(t.cipPressureBar, "bar"), t.cipPressureBar != null),
            KeyDataItem(6, "Caudal CIP disponible", Fmt.withUnit(t.cipFlowM3h, "m³/h"), t.cipFlowM3h != null),
            KeyDataItem(7, "Obstáculos internos", obstaclesText(t), obstaclesDeclared(t)),
        )
    }

    fun isComplete(t: TankEntity): Boolean = items(t).all { it.complete }

    fun completedCount(t: TankEntity): Int = items(t).count { it.complete }

    /**
     * Estado automático: Borrador ↔ Levantamiento completado según los 7 datos.
     * Los estados manuales (Pendiente de revisión / Revisado) se respetan.
     */
    fun autoStatus(t: TankEntity): String {
        val current = SurveyStatus.fromCode(t.status)
        return when (current) {
            SurveyStatus.PENDING_REVIEW, SurveyStatus.REVIEWED -> current.code
            else -> if (isComplete(t)) SurveyStatus.COMPLETED.code else SurveyStatus.DRAFT.code
        }
    }
}
