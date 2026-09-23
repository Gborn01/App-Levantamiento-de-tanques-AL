package com.jarabaimport.levantamiento.data.db

import android.content.ContentValues
import android.database.Cursor

/*
 * Mapeo TankEntity <-> SQLite. GENERADO a partir de los campos de TankEntity:
 * si agrega un campo, añádalo también aquí, en TANK_COLUMNS_SQL y cree una migración en DbHelper.
 */

internal val TANK_COLUMNS_SQL = listOf(
    "id INTEGER PRIMARY KEY AUTOINCREMENT",
    "uuid TEXT NOT NULL UNIQUE",
    "clientId INTEGER NOT NULL REFERENCES clients(id) ON DELETE CASCADE",
    "name TEXT NOT NULL DEFAULT ''",
    "code TEXT NOT NULL DEFAULT ''",
    "area TEXT NOT NULL DEFAULT ''",
    "process TEXT NOT NULL DEFAULT ''",
    "product TEXT NOT NULL DEFAULT ''",
    "productDescription TEXT NOT NULL DEFAULT ''",
    "residue TEXT NOT NULL DEFAULT ''",
    "residueCharacteristics TEXT NOT NULL DEFAULT ''",
    "residueOther TEXT NOT NULL DEFAULT ''",
    "productObservations TEXT NOT NULL DEFAULT ''",
    "internalDiameterMm REAL",
    "cylindricalHeightMm REAL",
    "totalHeightMm REAL",
    "nominalVolumeL REAL",
    "workingVolumeL REAL",
    "roofType TEXT NOT NULL DEFAULT ''",
    "bottomType TEXT NOT NULL DEFAULT ''",
    "bottomAngleDeg REAL",
    "material TEXT NOT NULL DEFAULT ''",
    "internalFinish TEXT NOT NULL DEFAULT ''",
    "cipFlowM3h REAL",
    "cipPressureBar REAL",
    "cipPressureSource TEXT NOT NULL DEFAULT ''",
    "cipTemperatureC REAL",
    "naohConcentrationPct REAL",
    "acidConcentrationPct REAL",
    "cipTimeMin REAL",
    "cipPipeDiameterIn REAL",
    "cipPipeLengthM REAL",
    "cipHasFilters INTEGER NOT NULL DEFAULT 0",
    "cipHasValves INTEGER NOT NULL DEFAULT 0",
    "cipHasElbows INTEGER NOT NULL DEFAULT 0",
    "cipHasRestrictions INTEGER NOT NULL DEFAULT 0",
    "cipOtherElements TEXT NOT NULL DEFAULT ''",
    "noObstacles INTEGER NOT NULL DEFAULT 0",
    "hasAgitator INTEGER NOT NULL DEFAULT 0",
    "agitatorDiameterMm REAL",
    "agitatorHeightMm REAL",
    "agitatorDescription TEXT NOT NULL DEFAULT ''",
    "hasCoil INTEGER NOT NULL DEFAULT 0",
    "coilType TEXT NOT NULL DEFAULT ''",
    "coilDiameterMm REAL",
    "coilDimensions TEXT NOT NULL DEFAULT ''",
    "coilDescription TEXT NOT NULL DEFAULT ''",
    "hasBaffles INTEGER NOT NULL DEFAULT 0",
    "bafflesCount INTEGER",
    "bafflesDimensions TEXT NOT NULL DEFAULT ''",
    "hasInternalTubes INTEGER NOT NULL DEFAULT 0",
    "hasSensors INTEGER NOT NULL DEFAULT 0",
    "hasInternalPiping INTEGER NOT NULL DEFAULT 0",
    "hasOtherObstacles INTEGER NOT NULL DEFAULT 0",
    "otherObstacles TEXT NOT NULL DEFAULT ''",
    "hasExistingHead INTEGER",
    "currentHeadModel TEXT NOT NULL DEFAULT ''",
    "headLocation TEXT NOT NULL DEFAULT ''",
    "headHeightAboveBottomMm REAL",
    "headConnectionType TEXT NOT NULL DEFAULT ''",
    "headConnectionDiameter TEXT NOT NULL DEFAULT ''",
    "headPosition TEXT NOT NULL DEFAULT ''",
    "headPositionOther TEXT NOT NULL DEFAULT ''",
    "observations TEXT NOT NULL DEFAULT ''",
    "technician TEXT NOT NULL DEFAULT ''",
    "clientRepresentative TEXT NOT NULL DEFAULT ''",
    "signaturePath TEXT NOT NULL DEFAULT ''",
    "surveyDate INTEGER NOT NULL DEFAULT 0",
    "status TEXT NOT NULL DEFAULT ''",
    "lastStep INTEGER NOT NULL DEFAULT 0",
    "createdAt INTEGER NOT NULL DEFAULT 0",
    "updatedAt INTEGER NOT NULL DEFAULT 0"
)

internal fun TankEntity.toValues(): ContentValues = ContentValues().apply {
    val t = this@toValues
        put("uuid", t.uuid)
        put("clientId", t.clientId)
        put("name", t.name)
        put("code", t.code)
        put("area", t.area)
        put("process", t.process)
        put("product", t.product)
        put("productDescription", t.productDescription)
        put("residue", t.residue)
        put("residueCharacteristics", t.residueCharacteristics)
        put("residueOther", t.residueOther)
        put("productObservations", t.productObservations)
        if (t.internalDiameterMm == null) putNull("internalDiameterMm") else put("internalDiameterMm", t.internalDiameterMm)
        if (t.cylindricalHeightMm == null) putNull("cylindricalHeightMm") else put("cylindricalHeightMm", t.cylindricalHeightMm)
        if (t.totalHeightMm == null) putNull("totalHeightMm") else put("totalHeightMm", t.totalHeightMm)
        if (t.nominalVolumeL == null) putNull("nominalVolumeL") else put("nominalVolumeL", t.nominalVolumeL)
        if (t.workingVolumeL == null) putNull("workingVolumeL") else put("workingVolumeL", t.workingVolumeL)
        put("roofType", t.roofType)
        put("bottomType", t.bottomType)
        if (t.bottomAngleDeg == null) putNull("bottomAngleDeg") else put("bottomAngleDeg", t.bottomAngleDeg)
        put("material", t.material)
        put("internalFinish", t.internalFinish)
        if (t.cipFlowM3h == null) putNull("cipFlowM3h") else put("cipFlowM3h", t.cipFlowM3h)
        if (t.cipPressureBar == null) putNull("cipPressureBar") else put("cipPressureBar", t.cipPressureBar)
        put("cipPressureSource", t.cipPressureSource)
        if (t.cipTemperatureC == null) putNull("cipTemperatureC") else put("cipTemperatureC", t.cipTemperatureC)
        if (t.naohConcentrationPct == null) putNull("naohConcentrationPct") else put("naohConcentrationPct", t.naohConcentrationPct)
        if (t.acidConcentrationPct == null) putNull("acidConcentrationPct") else put("acidConcentrationPct", t.acidConcentrationPct)
        if (t.cipTimeMin == null) putNull("cipTimeMin") else put("cipTimeMin", t.cipTimeMin)
        if (t.cipPipeDiameterIn == null) putNull("cipPipeDiameterIn") else put("cipPipeDiameterIn", t.cipPipeDiameterIn)
        if (t.cipPipeLengthM == null) putNull("cipPipeLengthM") else put("cipPipeLengthM", t.cipPipeLengthM)
        put("cipHasFilters", if (t.cipHasFilters) 1 else 0)
        put("cipHasValves", if (t.cipHasValves) 1 else 0)
        put("cipHasElbows", if (t.cipHasElbows) 1 else 0)
        put("cipHasRestrictions", if (t.cipHasRestrictions) 1 else 0)
        put("cipOtherElements", t.cipOtherElements)
        put("noObstacles", if (t.noObstacles) 1 else 0)
        put("hasAgitator", if (t.hasAgitator) 1 else 0)
        if (t.agitatorDiameterMm == null) putNull("agitatorDiameterMm") else put("agitatorDiameterMm", t.agitatorDiameterMm)
        if (t.agitatorHeightMm == null) putNull("agitatorHeightMm") else put("agitatorHeightMm", t.agitatorHeightMm)
        put("agitatorDescription", t.agitatorDescription)
        put("hasCoil", if (t.hasCoil) 1 else 0)
        put("coilType", t.coilType)
        if (t.coilDiameterMm == null) putNull("coilDiameterMm") else put("coilDiameterMm", t.coilDiameterMm)
        put("coilDimensions", t.coilDimensions)
        put("coilDescription", t.coilDescription)
        put("hasBaffles", if (t.hasBaffles) 1 else 0)
        if (t.bafflesCount == null) putNull("bafflesCount") else put("bafflesCount", t.bafflesCount)
        put("bafflesDimensions", t.bafflesDimensions)
        put("hasInternalTubes", if (t.hasInternalTubes) 1 else 0)
        put("hasSensors", if (t.hasSensors) 1 else 0)
        put("hasInternalPiping", if (t.hasInternalPiping) 1 else 0)
        put("hasOtherObstacles", if (t.hasOtherObstacles) 1 else 0)
        put("otherObstacles", t.otherObstacles)
        if (t.hasExistingHead == null) putNull("hasExistingHead") else put("hasExistingHead", if (t.hasExistingHead) 1 else 0)
        put("currentHeadModel", t.currentHeadModel)
        put("headLocation", t.headLocation)
        if (t.headHeightAboveBottomMm == null) putNull("headHeightAboveBottomMm") else put("headHeightAboveBottomMm", t.headHeightAboveBottomMm)
        put("headConnectionType", t.headConnectionType)
        put("headConnectionDiameter", t.headConnectionDiameter)
        put("headPosition", t.headPosition)
        put("headPositionOther", t.headPositionOther)
        put("observations", t.observations)
        put("technician", t.technician)
        put("clientRepresentative", t.clientRepresentative)
        put("signaturePath", t.signaturePath)
        put("surveyDate", t.surveyDate)
        put("status", t.status)
        put("lastStep", t.lastStep)
        put("createdAt", t.createdAt)
        put("updatedAt", t.updatedAt)
}

internal fun Cursor.toTank(): TankEntity {
    val c = this
    return TankEntity(
        id = c.long("id"),
        uuid = c.str("uuid"),
        clientId = c.long("clientId"),
        name = c.str("name"),
        code = c.str("code"),
        area = c.str("area"),
        process = c.str("process"),
        product = c.str("product"),
        productDescription = c.str("productDescription"),
        residue = c.str("residue"),
        residueCharacteristics = c.str("residueCharacteristics"),
        residueOther = c.str("residueOther"),
        productObservations = c.str("productObservations"),
        internalDiameterMm = c.dblOrNull("internalDiameterMm"),
        cylindricalHeightMm = c.dblOrNull("cylindricalHeightMm"),
        totalHeightMm = c.dblOrNull("totalHeightMm"),
        nominalVolumeL = c.dblOrNull("nominalVolumeL"),
        workingVolumeL = c.dblOrNull("workingVolumeL"),
        roofType = c.str("roofType"),
        bottomType = c.str("bottomType"),
        bottomAngleDeg = c.dblOrNull("bottomAngleDeg"),
        material = c.str("material"),
        internalFinish = c.str("internalFinish"),
        cipFlowM3h = c.dblOrNull("cipFlowM3h"),
        cipPressureBar = c.dblOrNull("cipPressureBar"),
        cipPressureSource = c.str("cipPressureSource"),
        cipTemperatureC = c.dblOrNull("cipTemperatureC"),
        naohConcentrationPct = c.dblOrNull("naohConcentrationPct"),
        acidConcentrationPct = c.dblOrNull("acidConcentrationPct"),
        cipTimeMin = c.dblOrNull("cipTimeMin"),
        cipPipeDiameterIn = c.dblOrNull("cipPipeDiameterIn"),
        cipPipeLengthM = c.dblOrNull("cipPipeLengthM"),
        cipHasFilters = c.bool("cipHasFilters"),
        cipHasValves = c.bool("cipHasValves"),
        cipHasElbows = c.bool("cipHasElbows"),
        cipHasRestrictions = c.bool("cipHasRestrictions"),
        cipOtherElements = c.str("cipOtherElements"),
        noObstacles = c.bool("noObstacles"),
        hasAgitator = c.bool("hasAgitator"),
        agitatorDiameterMm = c.dblOrNull("agitatorDiameterMm"),
        agitatorHeightMm = c.dblOrNull("agitatorHeightMm"),
        agitatorDescription = c.str("agitatorDescription"),
        hasCoil = c.bool("hasCoil"),
        coilType = c.str("coilType"),
        coilDiameterMm = c.dblOrNull("coilDiameterMm"),
        coilDimensions = c.str("coilDimensions"),
        coilDescription = c.str("coilDescription"),
        hasBaffles = c.bool("hasBaffles"),
        bafflesCount = c.intOrNull("bafflesCount"),
        bafflesDimensions = c.str("bafflesDimensions"),
        hasInternalTubes = c.bool("hasInternalTubes"),
        hasSensors = c.bool("hasSensors"),
        hasInternalPiping = c.bool("hasInternalPiping"),
        hasOtherObstacles = c.bool("hasOtherObstacles"),
        otherObstacles = c.str("otherObstacles"),
        hasExistingHead = c.boolOrNull("hasExistingHead"),
        currentHeadModel = c.str("currentHeadModel"),
        headLocation = c.str("headLocation"),
        headHeightAboveBottomMm = c.dblOrNull("headHeightAboveBottomMm"),
        headConnectionType = c.str("headConnectionType"),
        headConnectionDiameter = c.str("headConnectionDiameter"),
        headPosition = c.str("headPosition"),
        headPositionOther = c.str("headPositionOther"),
        observations = c.str("observations"),
        technician = c.str("technician"),
        clientRepresentative = c.str("clientRepresentative"),
        signaturePath = c.str("signaturePath"),
        surveyDate = c.long("surveyDate"),
        status = c.str("status"),
        lastStep = c.int("lastStep"),
        createdAt = c.long("createdAt"),
        updatedAt = c.long("updatedAt"),
    )
}
