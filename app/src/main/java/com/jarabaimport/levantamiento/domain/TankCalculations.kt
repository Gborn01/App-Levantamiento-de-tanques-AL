package com.jarabaimport.levantamiento.domain

import kotlin.math.PI

/**
 * Cálculos geométricos auxiliares. Son ESTIMACIONES y no sustituyen
 * el volumen nominal del fabricante.
 */
object TankCalculations {

    /** V = π × D² / 4 × H. Entradas en mm, resultado en litros. */
    fun cylindricalVolumeLiters(diameterMm: Double?, heightMm: Double?): Double? {
        if (diameterMm == null || heightMm == null || diameterMm <= 0 || heightMm <= 0) return null
        val dM = diameterMm / 1000.0
        val hM = heightMm / 1000.0
        val m3 = PI * dM * dM / 4.0 * hM
        return m3 * 1000.0
    }

    /** Relación altura / diámetro (H/D). */
    fun heightToDiameterRatio(heightMm: Double?, diameterMm: Double?): Double? {
        if (heightMm == null || diameterMm == null || heightMm <= 0 || diameterMm <= 0) return null
        return heightMm / diameterMm
    }

    /** Altura de referencia: cilíndrica si existe; si no, la total. */
    fun referenceHeightMm(cylindricalMm: Double?, totalMm: Double?): Double? =
        cylindricalMm?.takeIf { it > 0 } ?: totalMm?.takeIf { it > 0 }

    fun litersToM3(l: Double): Double = l / 1000.0
    fun m3ToLiters(m3: Double): Double = m3 * 1000.0
}
