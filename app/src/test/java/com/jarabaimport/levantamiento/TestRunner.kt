package com.jarabaimport.levantamiento

import com.jarabaimport.levantamiento.data.db.ClientEntity
import com.jarabaimport.levantamiento.data.db.TankEntity
import com.jarabaimport.levantamiento.data.sample.SampleTank
import com.jarabaimport.levantamiento.domain.Fmt
import com.jarabaimport.levantamiento.domain.KeyData
import com.jarabaimport.levantamiento.domain.TankCalculations
import com.jarabaimport.levantamiento.domain.TankDuplicator
import com.jarabaimport.levantamiento.domain.TankValidator
import com.jarabaimport.levantamiento.domain.model.ResidueCharacteristic
import com.jarabaimport.levantamiento.domain.model.SurveyStatus
import com.jarabaimport.levantamiento.domain.selection.NoSelectionYet
import com.jarabaimport.levantamiento.domain.selection.SelectionInput
import com.jarabaimport.levantamiento.export.CsvExporter
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.system.exitProcess

/*
 * Pruebas de la lógica de negocio (sin Android). Ejecutar con:  ./build.sh test
 * Mini-framework propio para no depender de librerías externas.
 */
private var passed = 0
private val failures = mutableListOf<String>()

private fun check(name: String, cond: Boolean, detail: () -> String = { "" }) {
    if (cond) passed++ else failures.add("$name ${detail()}")
}
private fun eq(name: String, expected: Any?, actual: Any?) = check(name, expected == actual) { "esperado=<$expected> obtenido=<$actual>" }
private fun near(name: String, expected: Double, actual: Double?, tol: Double) =
    check(name, actual != null && abs(expected - actual) <= tol) { "esperado≈$expected obtenido=$actual" }

fun main() {
    val sample = SampleTank.bepensaTk001(1)

    // Cálculos geométricos
    near("volumen D1500 H2000 = 3534 L", 3534.3, TankCalculations.cylindricalVolumeLiters(1500.0, 2000.0), 0.1)
    eq("volumen con D=0 es null", null, TankCalculations.cylindricalVolumeLiters(0.0, 2000.0))
    eq("volumen sin H es null", null, TankCalculations.cylindricalVolumeLiters(1500.0, null))
    near("H/D 2000/1500", 1.333, TankCalculations.heightToDiameterRatio(2000.0, 1500.0), 0.001)
    eq("altura de referencia usa total si no hay cilíndrica", 2500.0, TankCalculations.referenceHeightMm(null, 2500.0))

    // Duplicado
    eq("TK-001 → TK-002", "TK-002", TankDuplicator.nextCode("TK-001", listOf("TK-001")))
    eq("salta códigos usados", "TK-003", TankDuplicator.nextCode("TK-001", listOf("TK-001", "TK-002")))
    eq("T9 → T10", "T10", TankDuplicator.nextCode("T9", listOf("T9")))
    eq("sin número", "Mezclador-2", TankDuplicator.nextCode("Mezclador", listOf("Mezclador")))
    val d = TankDuplicator.duplicate(sample.copy(id = 5, status = "REVIEWED", signaturePath = "/f.png"), listOf("TK-001"))
    eq("duplicado id nuevo", 0L, d.id)
    eq("duplicado código", "TK-002", d.code)
    eq("duplicado conserva producto", "Mayonesa", d.product)
    eq("duplicado conserva diámetro", 1500.0, d.internalDiameterMm)
    eq("duplicado estado borrador", "DRAFT", d.status)
    eq("duplicado sin firma", "", d.signaturePath)
    check("duplicado uuid distinto", d.uuid != sample.uuid)

    // Validación
    val empty = TankEntity(clientId = 1)
    eq("nombre obligatorio", TankValidator.MSG_NAME, TankValidator.errorsForStep(empty, 0)["name"])
    eq("producto obligatorio", TankValidator.MSG_PRODUCT, TankValidator.errorsForStep(empty, 1)["product"])
    val bad = empty.copy(name = "A", product = "B", internalDiameterMm = 0.0, cylindricalHeightMm = 0.0, totalHeightMm = 100.0,
        nominalVolumeL = 1000.0, workingVolumeL = 2000.0, naohConcentrationPct = 150.0, hasOtherObstacles = true)
    val e = TankValidator.validateAll(bad)
    check("diámetro > 0", "internalDiameterMm" in e)
    check("altura > 0", "cylindricalHeightMm" in e)
    check("trabajo ≤ nominal", "workingVolumeL" in e)
    check("% ≤ 100", "naohConcentrationPct" in e)
    check("otros obstáculos requiere descripción", "otherObstacles" in e)
    eq("primer paso con error", 2, TankValidator.firstStepWithErrors(bad))
    val clean = TankValidator.sanitizeForStorage(bad)
    eq("autoguardado no guarda diámetro inválido", null, clean.internalDiameterMm)
    eq("autoguardado conserva texto", "A", clean.name)
    eq("autoguardado conserva valor válido", 1000.0, clean.nominalVolumeL)
    check("ejemplo sin errores", TankValidator.validateAll(sample).isEmpty()) { TankValidator.validateAll(sample).toString() }

    // 7 datos principales y estado
    check("ejemplo completo 7/7", KeyData.isComplete(sample))
    eq("estado automático completado", SurveyStatus.COMPLETED.code, KeyData.autoStatus(sample))
    eq("6/7 sin presión", 6, KeyData.completedCount(sample.copy(cipPressureBar = null)))
    eq("borrador si falta un dato", "DRAFT", KeyData.autoStatus(sample.copy(cipFlowM3h = null)))
    eq("respeta Revisado", "REVIEWED", KeyData.autoStatus(sample.copy(cipFlowM3h = null, status = "REVIEWED")))
    eq("obstáculos texto", "Agitador + Serpentín", KeyData.obstaclesText(sample))
    check("sin obstáculos cuenta como dato", KeyData.obstaclesDeclared(empty.copy(noObstacles = true)))
    check("residuo ≠ producto", KeyData.residueText(sample) != sample.product)
    eq("sin residuo → incompleto", 6, KeyData.completedCount(sample.copy(residue = "", residueCharacteristics = "")))

    // Características de residuo
    val set = setOf(ResidueCharacteristic.GREASY, ResidueCharacteristic.FILM)
    eq("codificar/decodificar", set, ResidueCharacteristic.decode(ResidueCharacteristic.encode(set)))

    // Números
    eq("coma decimal", 5.5, Fmt.parse("5,5"))
    eq("espacios", 1500.0, Fmt.parse(" 1500 "))
    eq("vacío", null, Fmt.parse(""))
    eq("punto solo", null, Fmt.parse("."))
    eq("miles", "1,500", Fmt.number(1500.0, 0))
    eq("con unidad", "5.5 bar", Fmt.withUnit(5.5, "bar"))

    // CSV
    val headers = CsvExporter.headers
    eq("columnas CSV únicas", headers.size, headers.toSet().size)
    val out = ByteArrayOutputStream()
    CsvExporter.write(listOf(Triple(ClientEntity(id = 1, name = "Bepensa, \"S.A.\""), sample, 3)), out)
    val bytes = out.toByteArray()
    check("CSV con BOM UTF-8", bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte())
    val lines = String(bytes, Charsets.UTF_8).removePrefix("﻿").split("\r\n").filter { it.isNotEmpty() }
    eq("CSV 2 líneas", 2, lines.size)
    check("CSV escapa comillas y comas", lines[1].contains("\"Bepensa, \"\"S.A.\"\"\""))
    val header = lines[0].split(",").map { it.trim('"') }
    val row = Regex("\"((?:[^\"]|\"\")*)\"").findAll(lines[1]).map { it.groupValues[1] }.toList()
    eq("CSV mismas columnas en fila y cabecera", header.size, row.size)
    fun col(n: String) = row[header.indexOf(n)]
    eq("CSV diámetro", "1500", col("diametro_interno_mm"))
    eq("CSV presión", "5.5", col("cip_presion_bar"))
    eq("CSV caudal", "8", col("cip_caudal_m3h"))
    eq("CSV volumen estimado", "3534", col("volumen_cilindrico_estimado_l"))
    eq("CSV H/D", "1.33", col("relacion_h_d"))
    eq("CSV agitador", "SI", col("agitador"))
    eq("CSV fotos", "3", col("cantidad_fotos"))
    eq("CSV datos 7", "7", col("datos_principales_completos_de_7"))

    // Selección futura: V1 no recomienda
    eq("V1 sin recomendaciones", 0, NoSelectionYet.evaluate(SelectionInput.from(sample)).candidates.size)

    println("Pruebas superadas: $passed")
    if (failures.isNotEmpty()) {
        println("FALLOS (${failures.size}):"); failures.forEach { println("  ✗ $it") }
        exitProcess(1)
    }
    println("TODAS LAS PRUEBAS OK")
}
