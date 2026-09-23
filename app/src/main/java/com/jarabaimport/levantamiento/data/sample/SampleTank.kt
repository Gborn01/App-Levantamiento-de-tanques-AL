package com.jarabaimport.levantamiento.data.sample

import com.jarabaimport.levantamiento.data.db.TankEntity
import com.jarabaimport.levantamiento.domain.model.PressureSource
import com.jarabaimport.levantamiento.domain.model.ResidueCharacteristic

/** Tanque de ejemplo (Bepensa · TK-001 · Mayonesa). Sin dependencias de Android (usable en pruebas). */
object SampleTank {
    fun bepensaTk001(clientId: Long) = TankEntity(
        clientId = clientId,
        name = "Tanque de mayonesa",
        code = "TK-001",
        area = "Planta de salsas",
        process = "Preparación",
        product = "Mayonesa",
        productDescription = "Emulsión de aceite, huevo y vinagre.",
        residue = "Película grasa y viscosa adherida a paredes y agitador después del vaciado.",
        residueCharacteristics = ResidueCharacteristic.encode(
            setOf(ResidueCharacteristic.GREASY, ResidueCharacteristic.VISCOUS, ResidueCharacteristic.FILM, ResidueCharacteristic.ADHERES)
        ),
        internalDiameterMm = 1500.0,
        cylindricalHeightMm = 2000.0,
        nominalVolumeL = 3500.0,
        roofType = "Toriesférico",
        bottomType = "Cónico",
        bottomAngleDeg = 15.0,
        material = "AISI 316L",
        internalFinish = "2B",
        cipFlowM3h = 8.0,
        cipPressureBar = 5.5,
        cipPressureSource = PressureSource.MEASURED_AT_HEAD.code,
        cipTemperatureC = 75.0,
        naohConcentrationPct = 1.5,
        acidConcentrationPct = 1.0,
        cipTimeMin = 45.0,
        cipPipeDiameterIn = 2.0,
        cipPipeLengthM = 25.0,
        hasAgitator = true,
        agitatorDiameterMm = 1000.0,
        agitatorDescription = "Agitador central de 1,000 mm de diámetro.",
        hasCoil = true,
        coilType = "Serpentín helicoidal",
        coilDescription = "Serpentín de calentamiento en la zona inferior.",
        hasExistingHead = false,
        observations = "Registro de ejemplo.",
        technician = "Técnico de ejemplo",
    )
}
