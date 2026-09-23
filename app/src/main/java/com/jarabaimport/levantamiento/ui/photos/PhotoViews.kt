package com.jarabaimport.levantamiento.ui.photos

import android.app.Dialog
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import com.jarabaimport.levantamiento.data.db.PhotoEntity
import com.jarabaimport.levantamiento.domain.model.PhotoCategory
import com.jarabaimport.levantamiento.ui.BaseActivity
import com.jarabaimport.levantamiento.ui.Palette
import com.jarabaimport.levantamiento.ui.card
import com.jarabaimport.levantamiento.ui.dp
import com.jarabaimport.levantamiento.ui.dpf
import com.jarabaimport.levantamiento.ui.makeButton
import com.jarabaimport.levantamiento.ui.makeText
import com.jarabaimport.levantamiento.ui.rounded
import com.jarabaimport.levantamiento.ui.row
import com.jarabaimport.levantamiento.ui.text
import com.jarabaimport.levantamiento.ui.weighted
import com.jarabaimport.levantamiento.util.ImageUtils

/** Carga una imagen reducida en segundo plano. */
fun BaseActivity.loadImage(iv: ImageView, path: String, side: Int) {
    iv.tag = path
    bg({ ImageUtils.decodeSampled(path, side) }) { r ->
        val b = r.getOrNull()
        if (b != null && iv.tag == path) iv.setImageBitmap(b)
    }
}

/**
 * Fotografías agrupadas por categoría (11 categorías).
 * editable = botones TOMAR FOTO / GALERÍA y posibilidad de borrar.
 */
fun BaseActivity.photoCategories(
    parent: ViewGroup,
    photos: List<PhotoEntity>,
    editable: Boolean,
    onTake: (PhotoCategory) -> Unit = {},
    onPick: (PhotoCategory) -> Unit = {},
    onDelete: (PhotoEntity) -> Unit = {},
) {
    val cats = if (editable) PhotoCategory.entries.toList()
    else PhotoCategory.entries.filter { cat -> photos.any { it.category == cat.code } }
    cats.forEachIndexed { i, cat ->
        val list = photos.filter { it.category == cat.code }
        parent.card(gapDp = 10) {
            text("${i + 1}. ${cat.label}" + if (list.isNotEmpty()) "  (${list.size})" else "", 18f, bold = true, color = Palette.NAVY, gapDp = 8)
            if (editable) row(8) {
                weighted(makeButton(context, "📷  Tomar foto") { onTake(cat) }, 2f)
                weighted(makeButton(context, "Galería", primary = false) { onPick(cat) }, 1f)
            }
            if (list.isNotEmpty()) {
                val strip = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
                list.forEach { p ->
                    val iv = ImageView(context).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        background = rounded(0xFFDDDDDD.toInt(), dpf(8f))
                        clipToOutline = true
                        contentDescription = cat.label
                        setOnClickListener { showPhoto(p, editable) { onDelete(p) } }
                    }
                    strip.addView(iv, LinearLayout.LayoutParams(dp(110), dp(110)).apply { marginEnd = dp(8) })
                    loadImage(iv, p.filePath, 300)
                }
                addView(HorizontalScrollView(context).apply { addView(strip) })
            }
        }
    }
}

/** Visor a pantalla completa con opción de borrar. */
fun BaseActivity.showPhoto(p: PhotoEntity, canDelete: Boolean, onDelete: () -> Unit) {
    val d = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
    val iv = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
    root.addView(iv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    loadImage(iv, p.filePath, 1600)
    val bar = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(0x99000000.toInt())
        setPadding(dp(16), dp(8), dp(8), dp(8))
    }
    bar.addView(makeText(this, PhotoCategory.fromCode(p.category).label, 18f, bold = true, color = Color.WHITE),
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    if (canDelete) bar.addView(makeText(this, "BORRAR", 16f, bold = true, color = Color.WHITE).apply {
        gravity = Gravity.CENTER
        setPadding(dp(16), 0, dp(16), 0)
        setOnClickListener {
            confirm("Borrar fotografía", "¿Desea borrar esta fotografía? No se puede deshacer.", "Borrar") { d.dismiss(); onDelete() }
        }
    }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(56)))
    bar.addView(makeText(this, "✕", 26f, bold = true, color = Color.WHITE).apply {
        gravity = Gravity.CENTER
        contentDescription = "Cerrar"
        setOnClickListener { d.dismiss() }
    }, LinearLayout.LayoutParams(dp(56), dp(56)))
    root.addView(bar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP))
    d.setContentView(root)
    d.show()
}
