package com.jarabaimport.levantamiento.export

/**
 * Identidad visual de los informes.
 *
 * - No se incluye ningún logo de terceros (p.ej. Alfa Laval).
 * - Para añadir un logo AUTORIZADO: copie un PNG a
 *   app/src/main/res/drawable/brand_logo.png y recompile. El PDF lo usará automáticamente.
 * - Para un segundo logo de socio autorizado: app/src/main/res/drawable/partner_logo.png
 */
object BrandingConfig {
    const val COMPANY_NAME = "Jaraba Import"
    const val REPORT_TITLE = "LEVANTAMIENTO TÉCNICO – SISTEMA DE LIMPIEZA DE TANQUE"
    const val REPORT_SUBTITLE = "Informe de campo para evaluación de sistema de limpieza"
    const val LOGO_RESOURCE_NAME = "brand_logo"
    const val PARTNER_LOGO_RESOURCE_NAME = "partner_logo"
    const val PRIMARY_COLOR = 0xFF002C77.toInt()
    const val ACCENT_COLOR = 0xFFE6EEF8.toInt()
    const val FOOTER_NOTE =
        "Documento de levantamiento de datos. No constituye una recomendación ni selección de equipo."
}
