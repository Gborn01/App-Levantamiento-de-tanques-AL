package com.jarabaimport.levantamiento.domain.model

/**
 * Enumeraciones del levantamiento. En la base de datos se guarda siempre [code]
 * (estable), nunca la etiqueta, para poder traducir o renombrar sin migraciones.
 */

enum class SurveyStatus(val code: String, val label: String) {
    DRAFT("DRAFT", "Borrador"),
    COMPLETED("COMPLETED", "Levantamiento completado"),
    PENDING_REVIEW("PENDING_REVIEW", "Pendiente de revisión"),
    REVIEWED("REVIEWED", "Revisado");

    companion object {
        fun fromCode(code: String?): SurveyStatus = entries.firstOrNull { it.code == code } ?: DRAFT
    }
}

enum class ResidueCharacteristic(val code: String, val label: String) {
    EASY("EASY", "Fácil de remover"),
    STICKY("STICKY", "Pegajoso"),
    GREASY("GREASY", "Graso"),
    VISCOUS("VISCOUS", "Viscoso"),
    DRY("DRY", "Seco"),
    PROTEIN("PROTEIN", "Proteico"),
    SUGARY("SUGARY", "Azucarado"),
    SOLIDS("SOLIDS", "Con sólidos"),
    FILM("FILM", "Forma película"),
    ADHERES("ADHERES", "Se adhiere a superficies"),
    HARD("HARD", "Difícil de remover"),
    OTHER("OTHER", "Otro");

    companion object {
        fun decode(csv: String): Set<ResidueCharacteristic> =
            csv.split(',').mapNotNull { c -> entries.firstOrNull { it.code == c.trim() } }.toSet()

        fun encode(set: Set<ResidueCharacteristic>): String =
            entries.filter { it in set }.joinToString(",") { it.code }
    }
}

enum class PhotoCategory(val code: String, val label: String) {
    EXTERIOR("EXTERIOR", "Vista exterior"),
    NAMEPLATE("NAMEPLATE", "Placa del tanque"),
    INTERIOR("INTERIOR", "Vista interior"),
    BOTTOM("BOTTOM", "Fondo"),
    ROOF("ROOF", "Techo"),
    AGITATOR("AGITATOR", "Agitador"),
    COIL("COIL", "Serpentín"),
    HEAD_CONNECTION("HEAD_CONNECTION", "Conexión del cabezal"),
    CIP_LINE("CIP_LINE", "Línea CIP"),
    GAUGE_CIP("GAUGE_CIP", "Manómetro durante CIP"),
    OTHER("OTHER", "Otras");

    companion object {
        fun fromCode(code: String?): PhotoCategory = entries.firstOrNull { it.code == code } ?: OTHER
    }
}

enum class HeadPosition(val code: String, val label: String) {
    CENTRAL("CENTRAL", "Central"),
    LATERAL("LATERAL", "Lateral"),
    TOP("TOP", "Superior"),
    OTHER("OTHER", "Otra");

    companion object {
        fun fromCode(code: String?): HeadPosition? = entries.firstOrNull { it.code == code }
    }
}

/** Origen del valor de presión CIP informado. Se prefiere la presión MEDIDA en la entrada del cabezal. */
enum class PressureSource(val code: String, val label: String) {
    MEASURED_AT_HEAD("MEASURED_AT_HEAD", "Medida en entrada del cabezal durante CIP"),
    MEASURED_OTHER("MEASURED_OTHER", "Medida en otro punto de la línea"),
    PUMP_NOMINAL("PUMP_NOMINAL", "Nominal de la bomba"),
    ESTIMATED("ESTIMATED", "Estimada / informada por el cliente");

    companion object {
        fun fromCode(code: String?): PressureSource? = entries.firstOrNull { it.code == code }
    }
}

/** Sugerencias para los desplegables (el usuario también puede escribir texto libre). */
object FieldOptions {
    val roofTypes = listOf("Plano", "Cónico", "Toriesférico", "Semielíptico", "Abierto", "Otro")
    val bottomTypes = listOf("Plano", "Plano inclinado", "Cónico", "Toriesférico", "Semielíptico", "Otro")
    val materials = listOf("AISI 304", "AISI 316", "AISI 316L", "Acero al carbono", "PRFV / Plástico", "Otro")
    val finishes = listOf("2B", "Pulido sanitario (Ra ≤ 0.8 µm)", "Pulido espejo", "Electropulido", "Esmerilado", "Desconocido")
    val connectionTypes = listOf("Clamp (Tri-Clamp)", "Rosca", "Brida", "Soldado", "DIN 11851", "SMS", "Otra")
    val coilTypes = listOf("Serpentín helicoidal", "Serpentín en placa", "Media caña (exterior)", "Otro")
    val processes = listOf("Preparación", "Almacenamiento", "Mezcla", "Fermentación", "Pasteurización", "Producto terminado", "CIP")
}
