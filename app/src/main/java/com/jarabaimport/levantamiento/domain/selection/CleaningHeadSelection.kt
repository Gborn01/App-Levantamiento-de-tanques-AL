package com.jarabaimport.levantamiento.domain.selection

import com.jarabaimport.levantamiento.data.db.TankEntity
import com.jarabaimport.levantamiento.domain.KeyData
import com.jarabaimport.levantamiento.domain.TankCalculations

/*
 * ==========================================================================
 *  PREPARACIÓN PARA LA FUTURA FASE "SELECCIÓN DE CABEZAL".
 *
 *  En V1 NO se realiza ninguna recomendación de modelo. Este archivo solo
 *  define el contrato (entrada/salida) para que en una fase posterior se
 *  pueda implementar un motor de reglas o una base de datos de productos
 *  sin tocar la captura de datos ni la UI existente.
 * ==========================================================================
 */

/** Datos normalizados que alimentarán el futuro módulo de selección. */
data class SelectionInput(
    val tankUuid: String,
    val product: String,
    val residue: String,
    val residueCharacteristics: String,
    val internalDiameterMm: Double?,
    val referenceHeightMm: Double?,
    val totalHeightMm: Double?,
    val heightToDiameter: Double?,
    val nominalVolumeL: Double?,
    val cipPressureBar: Double?,
    val cipPressureSource: String,
    val cipFlowM3h: Double?,
    val cipTemperatureC: Double?,
    val hasAgitator: Boolean,
    val agitatorDiameterMm: Double?,
    val hasCoil: Boolean,
    val hasBaffles: Boolean,
    val otherObstacles: Boolean,
    val headConnectionType: String,
    val headConnectionDiameter: String,
    val headPosition: String,
    val keyDataComplete: Boolean,
) {
    companion object {
        fun from(t: TankEntity): SelectionInput {
            val h = TankCalculations.referenceHeightMm(t.cylindricalHeightMm, t.totalHeightMm)
            return SelectionInput(
                tankUuid = t.uuid,
                product = t.product,
                residue = t.residue,
                residueCharacteristics = t.residueCharacteristics,
                internalDiameterMm = t.internalDiameterMm,
                referenceHeightMm = h,
                totalHeightMm = t.totalHeightMm,
                heightToDiameter = TankCalculations.heightToDiameterRatio(h, t.internalDiameterMm),
                nominalVolumeL = t.nominalVolumeL,
                cipPressureBar = t.cipPressureBar,
                cipPressureSource = t.cipPressureSource,
                cipFlowM3h = t.cipFlowM3h,
                cipTemperatureC = t.cipTemperatureC,
                hasAgitator = t.hasAgitator,
                agitatorDiameterMm = t.agitatorDiameterMm,
                hasCoil = t.hasCoil,
                hasBaffles = t.hasBaffles,
                otherObstacles = t.hasInternalTubes || t.hasSensors || t.hasInternalPiping || t.hasOtherObstacles,
                headConnectionType = t.headConnectionType,
                headConnectionDiameter = t.headConnectionDiameter,
                headPosition = t.headPosition,
                keyDataComplete = KeyData.isComplete(t),
            )
        }
    }
}

/** Resultado que devolverá el futuro motor de selección. */
data class HeadCandidate(
    val family: String,
    val model: String,
    val nozzleDiameterMm: Double?,
    val requiredPressureBar: Double?,
    val requiredFlowM3h: Double?,
    val coverage: String,
    val geometryCompatible: Boolean?,
    val obstaclesCompatible: Boolean?,
    val technicalNotes: List<String>,
)

data class SelectionResult(
    val candidates: List<HeadCandidate>,
    val warnings: List<String>,
)

/** Contrato del futuro módulo. */
interface CleaningHeadSelector {
    fun evaluate(input: SelectionInput): SelectionResult
}

/** Implementación de V1: no recomienda nada (deliberadamente). */
object NoSelectionYet : CleaningHeadSelector {
    override fun evaluate(input: SelectionInput) = SelectionResult(
        candidates = emptyList(),
        warnings = listOf("Módulo de selección no implementado en V1.")
    )
}
