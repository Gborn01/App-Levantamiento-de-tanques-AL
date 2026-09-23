package com.jarabaimport.levantamiento.ui.clients

import android.view.ViewGroup
import android.widget.LinearLayout
import com.jarabaimport.levantamiento.data.db.TankEntity
import com.jarabaimport.levantamiento.data.db.TankListItem
import com.jarabaimport.levantamiento.domain.Fmt
import com.jarabaimport.levantamiento.ui.Palette
import com.jarabaimport.levantamiento.ui.card
import com.jarabaimport.levantamiento.ui.makeStatusChip
import com.jarabaimport.levantamiento.ui.makeText
import com.jarabaimport.levantamiento.ui.row
import com.jarabaimport.levantamiento.ui.text
import com.jarabaimport.levantamiento.ui.weighted

/** Tarjeta de tanque: Código, Nombre, Producto, Volumen, Fecha y estado. */
fun ViewGroup.tankCard(
    code: String, name: String, product: String, volumeL: Double?, date: Long, status: String,
    clientName: String?, onClick: () -> Unit,
) = card(gapDp = 10, onClick = onClick) {
    row(4) {
        weighted(makeText(context, code.ifBlank { "(sin código)" }, 22f, bold = true, color = Palette.NAVY))
        addView(makeStatusChip(context, status), LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }
    if (clientName != null) text("Cliente: $clientName", 15f, bold = true, gapDp = 2)
    text("Nombre: " + name.ifBlank { "(sin nombre)" }, 17f, gapDp = 2)
    text("Producto: " + product.ifBlank { "—" }, 17f, gapDp = 2)
    row(0) {
        weighted(makeText(context, "Volumen: " + Fmt.withUnit(volumeL, "L", 0), 15f, color = Palette.MUTED))
        weighted(makeText(context, "Fecha: " + Fmt.date(date), 15f, color = Palette.MUTED))
    }
}

fun ViewGroup.tankCard(t: TankListItem, showClient: Boolean, onClick: () -> Unit) =
    tankCard(t.code, t.name, t.product, t.nominalVolumeL, t.surveyDate, t.status, if (showClient) t.clientName else null, onClick)

fun ViewGroup.tankCard(t: TankEntity, onClick: () -> Unit) =
    tankCard(t.code, t.name, t.product, t.nominalVolumeL, t.surveyDate, t.status, null, onClick)
