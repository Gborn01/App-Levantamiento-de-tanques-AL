package com.jarabaimport.levantamiento.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Formateo consistente: separador de miles "," y decimal "." (como se usa en R.D.). */
object Fmt {
    private val symbols = DecimalFormatSymbols(Locale.US)
    private fun df(pattern: String) = DecimalFormat(pattern, symbols)

    fun number(value: Double?, maxDecimals: Int = 2): String {
        if (value == null) return "—"
        val pattern = if (maxDecimals <= 0) "#,##0" else "#,##0." + "#".repeat(maxDecimals)
        return df(pattern).format(value)
    }

    fun withUnit(value: Double?, unit: String, maxDecimals: Int = 2): String =
        if (value == null) "—" else "${number(value, maxDecimals)} $unit"

    /** Número sin separador de miles para campos editables y CSV. */
    fun plain(value: Double?, maxDecimals: Int = 4): String {
        if (value == null) return ""
        val pattern = "0." + "#".repeat(maxDecimals)
        return df(pattern).format(value)
    }

    fun date(millis: Long?): String =
        if (millis == null) "—" else SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(millis))

    fun dateTime(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date(millis))

    fun fileStamp(millis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(millis))

    /** Convierte texto introducido por el usuario a número. Acepta coma decimal. */
    fun parse(text: String): Double? {
        val t = text.trim().replace(" ", "").replace(',', '.')
        if (t.isEmpty()) return null
        return t.toDoubleOrNull()
    }

    fun safeFileName(s: String): String =
        s.trim().replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_').ifEmpty { "sin_nombre" }.take(60)
}
