package com.jarabaimport.levantamiento.export

import com.jarabaimport.levantamiento.data.db.ClientEntity
import com.jarabaimport.levantamiento.data.db.TankEntity
import com.jarabaimport.levantamiento.domain.Fmt
import com.jarabaimport.levantamiento.domain.KeyData
import com.jarabaimport.levantamiento.domain.TankCalculations
import com.jarabaimport.levantamiento.domain.model.HeadPosition
import com.jarabaimport.levantamiento.domain.model.PressureSource
import com.jarabaimport.levantamiento.domain.model.ResidueCharacteristic
import com.jarabaimport.levantamiento.domain.model.SurveyStatus
import java.io.File
import java.io.OutputStream

/**
 * Exportación CSV con columnas ESTABLES (nombre_unidad), pensada para alimentar
 * posteriormente una herramienta de selección técnica.
 *
 * - Codificación UTF-8 con BOM (Excel abre bien los acentos).
 * - Separador: coma. Decimal: punto. Todos los campos entre comillas.
 * - Booleanos: SI / NO (vacío = no informado).
 * - Si se agregan columnas en el futuro, añadirlas AL FINAL y subir [SCHEMA_VERSION].
 */
object CsvExporter {

    const val SCHEMA_VERSION = "1"

    private class Col(val header: String, val value: (ClientEntity, TankEntity, Int) -> String)

    private fun n(v: Double?) = Fmt.plain(v)
    private fun b(v: Boolean) = if (v) "SI" else "NO"
    private fun b(v: Boolean?) = when (v) { true -> "SI"; false -> "NO"; null -> "" }

    private val columns: List<Col> = listOf(
        Col("schema_version") { _, _, _ -> SCHEMA_VERSION },
        Col("cliente_uuid") { c, _, _ -> c.uuid },
        Col("cliente") { c, _, _ -> c.name },
        Col("cliente_ciudad") { c, _, _ -> c.city },
        Col("tanque_uuid") { _, t, _ -> t.uuid },
        Col("tanque_codigo") { _, t, _ -> t.code },
        Col("tanque_nombre") { _, t, _ -> t.name },
        Col("area_planta") { _, t, _ -> t.area },
        Col("proceso") { _, t, _ -> t.process },
        Col("estado") { _, t, _ -> SurveyStatus.fromCode(t.status).label },
        Col("fecha_levantamiento") { _, t, _ -> Fmt.date(t.surveyDate) },
        Col("tecnico_responsable") { _, t, _ -> t.technician },
        Col("datos_principales_completos_de_7") { _, t, _ -> KeyData.completedCount(t).toString() },
        // Producto y residuo
        Col("producto") { _, t, _ -> t.product },
        Col("producto_descripcion") { _, t, _ -> t.productDescription },
        Col("residuo") { _, t, _ -> t.residue },
        Col("residuo_caracteristicas") { _, t, _ ->
            ResidueCharacteristic.decode(t.residueCharacteristics).joinToString("|") { it.label }
        },
        Col("residuo_otro") { _, t, _ -> t.residueOther },
        Col("producto_observaciones") { _, t, _ -> t.productObservations },
        // Geometría
        Col("diametro_interno_mm") { _, t, _ -> n(t.internalDiameterMm) },
        Col("altura_cilindrica_mm") { _, t, _ -> n(t.cylindricalHeightMm) },
        Col("altura_total_mm") { _, t, _ -> n(t.totalHeightMm) },
        Col("volumen_nominal_l") { _, t, _ -> n(t.nominalVolumeL) },
        Col("volumen_trabajo_l") { _, t, _ -> n(t.workingVolumeL) },
        Col("volumen_cilindrico_estimado_l") { _, t, _ ->
            n(TankCalculations.cylindricalVolumeLiters(t.internalDiameterMm, t.cylindricalHeightMm)?.let { Math.round(it).toDouble() })
        },
        Col("relacion_h_d") { _, t, _ ->
            val h = TankCalculations.referenceHeightMm(t.cylindricalHeightMm, t.totalHeightMm)
            TankCalculations.heightToDiameterRatio(h, t.internalDiameterMm)?.let { Fmt.plain(it, 2) } ?: ""
        },
        Col("tipo_techo") { _, t, _ -> t.roofType },
        Col("tipo_fondo") { _, t, _ -> t.bottomType },
        Col("angulo_fondo_grados") { _, t, _ -> n(t.bottomAngleDeg) },
        Col("material") { _, t, _ -> t.material },
        Col("acabado_interno") { _, t, _ -> t.internalFinish },
        // CIP
        Col("cip_caudal_m3h") { _, t, _ -> n(t.cipFlowM3h) },
        Col("cip_presion_bar") { _, t, _ -> n(t.cipPressureBar) },
        Col("cip_presion_origen") { _, t, _ -> PressureSource.fromCode(t.cipPressureSource)?.label ?: "" },
        Col("cip_temperatura_c") { _, t, _ -> n(t.cipTemperatureC) },
        Col("cip_naoh_pct") { _, t, _ -> n(t.naohConcentrationPct) },
        Col("cip_acido_pct") { _, t, _ -> n(t.acidConcentrationPct) },
        Col("cip_tiempo_min") { _, t, _ -> n(t.cipTimeMin) },
        Col("cip_tuberia_diametro_pulg") { _, t, _ -> n(t.cipPipeDiameterIn) },
        Col("cip_tuberia_longitud_m") { _, t, _ -> n(t.cipPipeLengthM) },
        Col("cip_filtros") { _, t, _ -> b(t.cipHasFilters) },
        Col("cip_valvulas") { _, t, _ -> b(t.cipHasValves) },
        Col("cip_codos") { _, t, _ -> b(t.cipHasElbows) },
        Col("cip_restricciones") { _, t, _ -> b(t.cipHasRestrictions) },
        Col("cip_otros_elementos") { _, t, _ -> t.cipOtherElements },
        // Internos
        Col("sin_obstaculos") { _, t, _ -> b(t.noObstacles) },
        Col("obstaculos_resumen") { _, t, _ -> KeyData.obstaclesText(t) },
        Col("agitador") { _, t, _ -> b(t.hasAgitator) },
        Col("agitador_diametro_mm") { _, t, _ -> n(t.agitatorDiameterMm) },
        Col("agitador_altura_mm") { _, t, _ -> n(t.agitatorHeightMm) },
        Col("agitador_descripcion") { _, t, _ -> t.agitatorDescription },
        Col("serpentin") { _, t, _ -> b(t.hasCoil) },
        Col("serpentin_tipo") { _, t, _ -> t.coilType },
        Col("serpentin_diametro_mm") { _, t, _ -> n(t.coilDiameterMm) },
        Col("serpentin_dimensiones") { _, t, _ -> t.coilDimensions },
        Col("serpentin_descripcion") { _, t, _ -> t.coilDescription },
        Col("baffles") { _, t, _ -> b(t.hasBaffles) },
        Col("baffles_cantidad") { _, t, _ -> t.bafflesCount?.toString() ?: "" },
        Col("baffles_dimensiones") { _, t, _ -> t.bafflesDimensions },
        Col("tubos_internos") { _, t, _ -> b(t.hasInternalTubes) },
        Col("sensores") { _, t, _ -> b(t.hasSensors) },
        Col("tuberias_internas") { _, t, _ -> b(t.hasInternalPiping) },
        Col("otros_obstaculos") { _, t, _ -> b(t.hasOtherObstacles) },
        Col("otros_obstaculos_descripcion") { _, t, _ -> t.otherObstacles },
        // Cabezal
        Col("cabezal_existente") { _, t, _ -> b(t.hasExistingHead) },
        Col("cabezal_modelo_actual") { _, t, _ -> t.currentHeadModel },
        Col("cabezal_ubicacion") { _, t, _ -> t.headLocation },
        Col("cabezal_altura_sobre_fondo_mm") { _, t, _ -> n(t.headHeightAboveBottomMm) },
        Col("cabezal_tipo_conexion") { _, t, _ -> t.headConnectionType },
        Col("cabezal_diametro_conexion") { _, t, _ -> t.headConnectionDiameter },
        Col("cabezal_posicion") { _, t, _ ->
            val p = HeadPosition.fromCode(t.headPosition)
            if (p == HeadPosition.OTHER && t.headPositionOther.isNotBlank()) "Otra: ${t.headPositionOther}" else p?.label ?: ""
        },
        // Cierre
        Col("observaciones") { _, t, _ -> t.observations },
        Col("representante_cliente") { _, t, _ -> t.clientRepresentative },
        Col("firma_registrada") { _, t, _ -> b(t.signaturePath.isNotBlank()) },
        Col("cantidad_fotos") { _, _, photos -> photos.toString() },
        Col("fecha_actualizacion") { _, t, _ -> Fmt.dateTime(t.updatedAt) },
    )

    val headers: List<String> get() = columns.map { it.header }

    fun escape(value: String): String = "\"" + value.replace("\"", "\"\"").replace("\r\n", "\n") + "\""

    /** rows: (cliente, tanque, cantidad de fotos) */
    fun write(rows: List<Triple<ClientEntity, TankEntity, Int>>, out: OutputStream) {
        val w = out.bufferedWriter(Charsets.UTF_8)
        w.write("﻿")
        w.write(columns.joinToString(",") { escape(it.header) })
        w.write("\r\n")
        for ((c, t, p) in rows) {
            w.write(columns.joinToString(",") { escape(it.value(c, t, p)) })
            w.write("\r\n")
        }
        w.flush()
    }

    fun writeToFile(rows: List<Triple<ClientEntity, TankEntity, Int>>, file: File) {
        file.outputStream().use { write(rows, it) }
    }
}
