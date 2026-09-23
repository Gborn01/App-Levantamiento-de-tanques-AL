package com.jarabaimport.levantamiento.data.db

import java.util.UUID

/**
 * Tanque + su levantamiento técnico.
 *
 * UNIDADES NORMALIZADAS (siempre se guardan así, la UI solo convierte al mostrar):
 *  - Dimensiones: mm
 *  - Volumen: litros (L)
 *  - Ángulo: grados
 *  - Caudal: m³/h
 *  - Presión: bar
 *  - Temperatura: °C
 *  - Concentraciones: %
 *  - Tiempo: min
 *  - Diámetro de tubería CIP: pulgadas
 *  - Longitud de tubería CIP: m
 *
 * Los campos numéricos son nullable: null = "no medido / no informado".
 */
data class TankEntity(
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val clientId: Long,

    // ---- Paso 1: Identificación ----
    val name: String = "",
    val code: String = "",
    val area: String = "",
    val process: String = "",

    // ---- Paso 2: Producto y residuo ----
    val product: String = "",
    val productDescription: String = "",
    /** Descripción libre del residuo después del vaciado. NO es igual al producto. */
    val residue: String = "",
    /** Códigos de [com.jarabaimport.levantamiento.domain.model.ResidueCharacteristic] separados por coma. */
    val residueCharacteristics: String = "",
    val residueOther: String = "",
    val productObservations: String = "",

    // ---- Paso 3: Geometría ----
    val internalDiameterMm: Double? = null,
    val cylindricalHeightMm: Double? = null,
    val totalHeightMm: Double? = null,
    val nominalVolumeL: Double? = null,
    val workingVolumeL: Double? = null,
    val roofType: String = "",
    val bottomType: String = "",
    val bottomAngleDeg: Double? = null,
    val material: String = "",
    val internalFinish: String = "",

    // ---- Paso 4: CIP ----
    val cipFlowM3h: Double? = null,
    val cipPressureBar: Double? = null,
    /** Código de [com.jarabaimport.levantamiento.domain.model.PressureSource]. */
    val cipPressureSource: String = "",
    val cipTemperatureC: Double? = null,
    val naohConcentrationPct: Double? = null,
    val acidConcentrationPct: Double? = null,
    val cipTimeMin: Double? = null,
    val cipPipeDiameterIn: Double? = null,
    val cipPipeLengthM: Double? = null,
    val cipHasFilters: Boolean = false,
    val cipHasValves: Boolean = false,
    val cipHasElbows: Boolean = false,
    val cipHasRestrictions: Boolean = false,
    val cipOtherElements: String = "",

    // ---- Paso 5: Internos / obstáculos ----
    /** El técnico confirma explícitamente que el tanque NO tiene obstáculos internos. */
    val noObstacles: Boolean = false,
    val hasAgitator: Boolean = false,
    val agitatorDiameterMm: Double? = null,
    val agitatorHeightMm: Double? = null,
    val agitatorDescription: String = "",
    val hasCoil: Boolean = false,
    val coilType: String = "",
    val coilDiameterMm: Double? = null,
    val coilDimensions: String = "",
    val coilDescription: String = "",
    val hasBaffles: Boolean = false,
    val bafflesCount: Int? = null,
    val bafflesDimensions: String = "",
    val hasInternalTubes: Boolean = false,
    val hasSensors: Boolean = false,
    val hasInternalPiping: Boolean = false,
    val hasOtherObstacles: Boolean = false,
    val otherObstacles: String = "",

    // ---- Paso 6: Cabezal ----
    /** null = no informado; true = sí existe; false = no existe. */
    val hasExistingHead: Boolean? = null,
    val currentHeadModel: String = "",
    val headLocation: String = "",
    val headHeightAboveBottomMm: Double? = null,
    val headConnectionType: String = "",
    val headConnectionDiameter: String = "",
    /** Código de [com.jarabaimport.levantamiento.domain.model.HeadPosition]. */
    val headPosition: String = "",
    val headPositionOther: String = "",

    // ---- Paso 8: Observaciones y firma ----
    val observations: String = "",
    val technician: String = "",
    val clientRepresentative: String = "",
    /** Ruta absoluta del PNG de firma (almacenamiento interno de la app). */
    val signaturePath: String = "",
    val surveyDate: Long = System.currentTimeMillis(),

    // ---- Control ----
    /** Código de [com.jarabaimport.levantamiento.domain.model.SurveyStatus]. */
    val status: String = "DRAFT",
    /** Último paso visitado en el asistente (para retomar). */
    val lastStep: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
