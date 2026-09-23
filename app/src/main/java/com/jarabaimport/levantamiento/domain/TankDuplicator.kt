package com.jarabaimport.levantamiento.domain

import com.jarabaimport.levantamiento.data.db.TankEntity
import com.jarabaimport.levantamiento.domain.model.SurveyStatus
import java.util.UUID

/** Lógica de "DUPLICAR TANQUE": copia todos los datos técnicos y propone el siguiente código. */
object TankDuplicator {

    private val trailingNumber = Regex("^(.*?)(\\d+)$")

    /** TK-001 → TK-002 (conserva ceros a la izquierda). Evita códigos ya usados. */
    fun nextCode(code: String, existing: Collection<String>): String {
        val used = existing.map { it.trim().uppercase() }.toSet()
        val m = trailingNumber.find(code.trim())
        if (m == null) {
            val base = code.trim().ifEmpty { "TK" }
            var i = 2
            while ("$base-$i".uppercase() in used) i++
            return "$base-$i"
        }
        val prefix = m.groupValues[1]
        val digits = m.groupValues[2]
        var n = digits.toLong() + 1
        while (true) {
            val candidate = prefix + n.toString().padStart(digits.length, '0')
            if (candidate.uppercase() !in used) return candidate
            n++
        }
    }

    /** Copia sin id, fotos ni firma; estado Borrador; fecha actual. */
    fun duplicate(source: TankEntity, existingCodes: Collection<String>, now: Long = System.currentTimeMillis()): TankEntity {
        val newCode = nextCode(source.code, existingCodes)
        val newName = if (source.code.isNotBlank() && source.name.contains(source.code)) {
            source.name.replace(source.code, newCode)
        } else source.name
        return source.copy(
            id = 0,
            uuid = UUID.randomUUID().toString(),
            code = newCode,
            name = newName,
            signaturePath = "",
            status = SurveyStatus.DRAFT.code,
            lastStep = 0,
            surveyDate = now,
            createdAt = now,
            updatedAt = now,
        )
    }
}
