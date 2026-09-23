package com.jarabaimport.levantamiento.ui

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import com.jarabaimport.levantamiento.domain.Fmt
import com.jarabaimport.levantamiento.domain.model.SurveyStatus

/*
 * Kit de interfaz propio (vistas nativas de Android, sin librerías).
 * Estética: fondo claro, azul corporativo profundo, grises suaves, líneas limpias.
 * Todo con objetivos táctiles grandes (≥ 56–60 dp) para uso en campo, incluso con guantes.
 * Para ajustar la identidad visual cambie solo [Palette].
 */
object Palette {
    const val NAVY = 0xFF002C77.toInt()          // azul corporativo principal
    const val NAVY_DARK = 0xFF001A4D.toInt()
    const val ACCENT = 0xFF3D8FD8.toInt()        // acento
    const val SKY = 0xFFE6EEF8.toInt()           // azul muy claro
    const val SURFACE = 0xFFF4F6F9.toInt()       // gris muy claro para tarjetas
    const val WHITE = Color.WHITE
    const val TEXT = 0xFF111111.toInt()
    const val MUTED = 0xFF4A5058.toInt()
    const val OUTLINE = 0xFFB8BEC6.toInt()
    const val ERROR = 0xFFB3261E.toInt()
    const val KEY_BG = 0xFFFFF6DE.toInt()        // resaltado "dato principal"
    const val KEY_BORDER = 0xFFE0A800.toInt()
    const val OK = 0xFF1B7832.toInt()
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
fun Context.dpf(v: Float): Float = v * resources.displayMetrics.density

fun rounded(color: Int, radiusPx: Float, strokeColor: Int? = null, strokePx: Int = 0): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radiusPx
        if (strokeColor != null && strokePx > 0) setStroke(strokePx, strokeColor)
    }

fun ripple(content: Drawable, radiusPx: Float, rippleColor: Int = 0x33002C77): Drawable =
    RippleDrawable(ColorStateList.valueOf(rippleColor), content, rounded(Color.BLACK, radiusPx))

val lpMatchWrap get() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

fun LinearLayout.LayoutParams.margins(top: Int = 0, bottom: Int = 0, start: Int = 0, end: Int = 0) = apply {
    topMargin = top; bottomMargin = bottom; marginStart = start; marginEnd = end
}

/** Añade una vista con separación inferior estándar. */
fun <T : View> ViewGroup.addWithGap(v: T, gapDp: Int = 12, lp: ViewGroup.LayoutParams? = null): T {
    val params = (lp as? LinearLayout.LayoutParams) ?: lpMatchWrap
    params.bottomMargin = context.dp(gapDp)
    addView(v, params)
    return v
}

fun ViewGroup.vertical(gapDp: Int = 12, body: LinearLayout.() -> Unit = {}): LinearLayout {
    val l = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    addWithGap(l, gapDp)
    l.body()
    return l
}

/** Fila horizontal; los hijos añadidos con [weighted] se reparten el ancho. */
fun ViewGroup.row(gapDp: Int = 12, body: LinearLayout.() -> Unit): LinearLayout {
    val l = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    addWithGap(l, gapDp)
    l.body()
    return l
}

fun <T : View> LinearLayout.weighted(v: T, weight: Float = 1f, spacingDp: Int = 10): T {
    val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
    if (childCount > 0) lp.marginStart = context.dp(spacingDp)
    addView(v, lp)
    return v
}

fun ViewGroup.text(
    s: CharSequence,
    sizeSp: Float = 17f,
    bold: Boolean = false,
    color: Int = Palette.TEXT,
    gapDp: Int = 6,
    allCaps: Boolean = false,
): TextView {
    val tv = makeText(context, s, sizeSp, bold, color, allCaps)
    addWithGap(tv, gapDp)
    return tv
}

fun makeText(ctx: Context, s: CharSequence, sizeSp: Float = 17f, bold: Boolean = false, color: Int = Palette.TEXT, allCaps: Boolean = false) =
    TextView(ctx).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setAllCaps(allCaps)
    }

fun ViewGroup.sectionTitle(s: String, gapDp: Int = 8) =
    text(s.uppercase(), 16f, bold = true, color = Palette.NAVY, gapDp = gapDp).apply { letterSpacing = 0.04f }

/** Botón grande (≥ 60 dp). primary = relleno azul; si no, contorno azul sobre blanco. */
fun makeButton(ctx: Context, label: String, primary: Boolean = true, enabled: Boolean = true, onClick: () -> Unit): Button =
    Button(ctx).apply {
        text = label.uppercase()
        setAllCaps(true)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        typeface = Typeface.DEFAULT_BOLD
        minHeight = ctx.dp(60)
        minimumHeight = ctx.dp(60)
        stateListAnimator = null
        setPadding(ctx.dp(14), ctx.dp(10), ctx.dp(14), ctx.dp(10))
        val r = ctx.dpf(12f)
        if (primary) {
            background = ripple(rounded(if (enabled) Palette.NAVY else 0xFFB0B8C4.toInt(), r), r, 0x55FFFFFF)
            setTextColor(Color.WHITE)
        } else {
            background = ripple(rounded(Color.WHITE, r, if (enabled) Palette.NAVY else Palette.OUTLINE, ctx.dp(2)), r)
            setTextColor(if (enabled) Palette.NAVY else Palette.OUTLINE)
        }
        isEnabled = enabled
        setOnClickListener { onClick() }
    }

fun ViewGroup.bigButton(label: String, primary: Boolean = true, enabled: Boolean = true, gapDp: Int = 12, onClick: () -> Unit): Button =
    addWithGap(makeButton(context, label, primary, enabled, onClick), gapDp)

/** Tarjeta con fondo gris claro (o amarillo suave si es "dato principal"). */
fun ViewGroup.card(highlight: Boolean = false, gapDp: Int = 14, onClick: (() -> Unit)? = null, body: LinearLayout.() -> Unit): LinearLayout {
    val c = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val p = context.dp(16)
        setPadding(p, p, p, p)
        val r = context.dpf(14f)
        val bg = if (highlight) rounded(Palette.KEY_BG, r, Palette.KEY_BORDER, context.dp(2)) else rounded(Palette.SURFACE, r)
        background = if (onClick != null) ripple(bg, r) else bg
        if (onClick != null) {
            isClickable = true
            setOnClickListener { onClick() }
        }
    }
    addWithGap(c, gapDp)
    c.body()
    return c
}

fun ViewGroup.sectionCard(title: String, highlight: Boolean = false, body: LinearLayout.() -> Unit): LinearLayout =
    card(highlight) {
        sectionTitle(title, 10)
        body()
    }

/** Campo de formulario: etiqueta + entrada (+ sufijo de unidad) + mensaje de error/ayuda. */
class Field(val container: LinearLayout, val input: EditText, private val msg: TextView, private val help: String?) {
    private val ctx = container.context
    fun setError(error: String?) {
        if (error != null) {
            msg.text = error
            msg.setTextColor(Palette.ERROR)
            msg.visibility = View.VISIBLE
            input.background = inputBg(ctx, error = true)
        } else {
            input.background = inputBg(ctx, error = false)
            if (help != null) {
                msg.text = help
                msg.setTextColor(Palette.MUTED)
                msg.visibility = View.VISIBLE
            } else msg.visibility = View.GONE
        }
    }
}

private fun inputBg(ctx: Context, error: Boolean) =
    rounded(Color.WHITE, ctx.dpf(10f), if (error) Palette.ERROR else Palette.OUTLINE, ctx.dp(if (error) 3 else 2))

private fun ViewGroup.labelView(label: String, isKey: Boolean, required: Boolean): TextView {
    val tv = makeText(context, (if (isKey) "★ " else "") + label + (if (required) " *" else ""), 15f, bold = true,
        color = if (isKey) 0xFF7A5500.toInt() else Palette.MUTED)
    addWithGap(tv, 4)
    return tv
}

private fun ViewGroup.baseField(
    label: String,
    value: String,
    inputType: Int,
    isKey: Boolean,
    required: Boolean,
    multiline: Boolean,
    hint: String?,
    help: String?,
    suffix: String?,
    trailing: View? = null,
    filter: ((String) -> String)? = null,
    onChange: (String) -> Unit,
): Field {
    val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    addWithGap(box, 14)
    box.labelView(label, isKey, required)
    val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    box.addView(row, lpMatchWrap)
    val et = EditText(context).apply {
        setText(value)
        this.inputType = inputType or (if (multiline) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        setTextColor(Palette.TEXT)
        setHintTextColor(0xFF9AA0A6.toInt())
        if (hint != null) this.hint = hint
        val p = context.dp(14)
        setPadding(p, p, p, p)
        minHeight = context.dp(56)
        background = inputBg(context, false)
        if (multiline) {
            minLines = 3
            gravity = Gravity.TOP or Gravity.START
            setSingleLine(false)
        } else setSingleLine(true)
    }
    row.addView(et, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    if (suffix != null) {
        row.addView(makeText(context, suffix, 17f, bold = true, color = Palette.NAVY).apply {
            setPadding(context.dp(10), 0, context.dp(4), 0)
        })
    }
    if (trailing != null) row.addView(trailing)
    val msg = makeText(context, "", 14f, color = Palette.MUTED).apply { visibility = View.GONE; setPadding(context.dp(4), context.dp(4), 0, 0) }
    box.addView(msg)
    et.addTextChangedListener(object : TextWatcher {
        private var selfEdit = false
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (selfEdit) return
            val raw = s?.toString() ?: ""
            val clean = filter?.invoke(raw) ?: raw
            if (clean != raw) {
                selfEdit = true
                s?.replace(0, s.length, clean)
                selfEdit = false
            }
            onChange(clean)
        }
    })
    return Field(box, et, msg, help).also { it.setError(null) }
}

fun ViewGroup.textField(
    label: String,
    value: String,
    required: Boolean = false,
    multiline: Boolean = false,
    isKey: Boolean = false,
    hint: String? = null,
    help: String? = null,
    inputType: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
    onChange: (String) -> Unit,
): Field = baseField(label, value, inputType, isKey, required, multiline, hint, help, null, onChange = onChange)

/** Campo numérico con teclado numérico. No admite negativos. Acepta coma o punto decimal. */
fun ViewGroup.numberField(
    label: String,
    unit: String,
    value: Double?,
    isKey: Boolean = false,
    integer: Boolean = false,
    help: String? = null,
    onChange: (Double?) -> Unit,
): Field {
    val type = if (integer) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    val filter: (String) -> String = { s -> s.filter { it.isDigit() || (!integer && (it == '.' || it == ',')) } }
    return baseField(label, if (integer) value?.toLong()?.toString() ?: "" else Fmt.plain(value), type, isKey, false, false,
        null, help, unit, filter = filter) { onChange(Fmt.parse(it)) }
}

/** Campo de texto con lista de opciones (▼). El usuario puede elegir o escribir libremente. */
fun ViewGroup.suggestField(label: String, value: String, options: List<String>, isKey: Boolean = false, onChange: (String) -> Unit): Field {
    lateinit var field: Field
    val btn = TextView(context).apply {
        text = "▼"
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        setTextColor(Palette.NAVY)
        gravity = Gravity.CENTER
        background = ripple(rounded(Palette.SKY, context.dpf(10f)), context.dpf(10f))
        layoutParams = LinearLayout.LayoutParams(context.dp(60), context.dp(56)).apply { marginStart = context.dp(8) }
        contentDescription = "Opciones de $label"
        setOnClickListener {
            AlertDialog.Builder(context).setTitle(label)
                .setItems(options.toTypedArray()) { _, i -> field.input.setText(options[i]); field.input.setSelection(options[i].length) }
                .setNegativeButton("Cancelar", null).show()
        }
    }
    field = baseField(label, value, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, isKey, false, false,
        "Elija ▼ o escriba", null, null, trailing = btn, onChange = onChange)
    return field
}

/** Casilla grande: toda la fila es táctil. */
fun ViewGroup.checkRow(label: String, checked: Boolean, gapDp: Int = 4, onChange: (Boolean) -> Unit): CheckBox {
    val cb = CheckBox(context).apply {
        text = label
        isChecked = checked
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        setTextColor(Palette.TEXT)
        buttonTintList = ColorStateList.valueOf(Palette.NAVY)
        minHeight = context.dp(56)
        setPadding(context.dp(10), 0, 0, 0)
        val r = context.dpf(10f)
        fun paint(c: Boolean) { background = ripple(rounded(if (c) Palette.SKY else Color.TRANSPARENT, r), r) }
        paint(checked)
        setOnCheckedChangeListener { _, c -> paint(c); onChange(c) }
    }
    addWithGap(cb, gapDp)
    return cb
}

/** Grupo de opción única con filas grandes. onSelect(null) si se desmarca. */
fun <T> ViewGroup.radioList(options: List<T>, selected: T?, label: (T) -> String, allowDeselect: Boolean = true, onSelect: (T?) -> Unit): LinearLayout {
    val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    addWithGap(box, 10)
    val buttons = mutableListOf<RadioButton>()
    var current = selected
    val r = context.dpf(10f)
    fun refresh() {
        buttons.forEachIndexed { i, b ->
            val sel = options[i] == current
            b.isChecked = sel
            b.background = ripple(rounded(if (sel) Palette.SKY else Color.TRANSPARENT, r), r)
        }
    }
    options.forEach { opt ->
        val rb = RadioButton(context).apply {
            text = label(opt)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(Palette.TEXT)
            buttonTintList = ColorStateList.valueOf(Palette.NAVY)
            minHeight = context.dp(56)
            setPadding(context.dp(10), 0, 0, 0)
            setOnClickListener {
                current = if (current == opt && allowDeselect) null else opt
                refresh()
                onSelect(current)
            }
        }
        buttons.add(rb)
        box.addView(rb, lpMatchWrap.margins(bottom = context.dp(2)))
    }
    refresh()
    return box
}

fun makeStatusChip(ctx: Context, statusCode: String): TextView {
    val s = SurveyStatus.fromCode(statusCode)
    val (bg, fg) = when (s) {
        SurveyStatus.DRAFT -> 0xFFEDEDED.toInt() to 0xFF444444.toInt()
        SurveyStatus.COMPLETED -> 0xFFD9F2DF.toInt() to Palette.OK
        SurveyStatus.PENDING_REVIEW -> 0xFFFFF0CC.toInt() to 0xFF8A5A00.toInt()
        SurveyStatus.REVIEWED -> Palette.SKY to Palette.NAVY
    }
    return makeText(ctx, s.label, 13f, bold = true, color = fg).apply {
        background = rounded(bg, ctx.dpf(20f))
        setPadding(ctx.dp(10), ctx.dp(4), ctx.dp(10), ctx.dp(4))
    }
}

fun ViewGroup.labeledValue(label: String, value: String, gapDp: Int = 10) {
    val box = vertical(gapDp)
    box.addView(makeText(context, label.uppercase(), 12f, bold = true, color = Palette.MUTED))
    box.addView(makeText(context, value.ifBlank { "—" }, 18f, bold = true, color = Palette.TEXT))
}

/** Casilla de valor destacado (resumen). */
fun makeTile(ctx: Context, label: String, value: String, key: Boolean = false, onClick: (() -> Unit)? = null): LinearLayout =
    LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        val p = ctx.dp(12)
        setPadding(p, p, p, p)
        val r = ctx.dpf(12f)
        val bg = rounded(Palette.SKY, r, if (key) Palette.KEY_BORDER else null, if (key) ctx.dp(2) else 0)
        background = if (onClick != null) ripple(bg, r) else bg
        if (onClick != null) setOnClickListener { onClick() }
        minimumHeight = ctx.dp(76)
        addView(makeText(ctx, (if (key) "★ " else "") + label.uppercase(), 12f, bold = true, color = Palette.MUTED))
        addView(makeText(ctx, value.ifBlank { "—" }, 20f, bold = true, color = Palette.NAVY))
    }

fun ViewGroup.divider(gapDp: Int = 12) {
    val v = View(context).apply { setBackgroundColor(0xFFE1E5EA.toInt()) }
    addView(v, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dp(1)).margins(bottom = context.dp(gapDp)))
}

fun ViewGroup.spacer(dp: Int) {
    addView(View(context), LinearLayout.LayoutParams(1, context.dp(dp)))
}

fun frame(ctx: Context) = FrameLayout(ctx)
