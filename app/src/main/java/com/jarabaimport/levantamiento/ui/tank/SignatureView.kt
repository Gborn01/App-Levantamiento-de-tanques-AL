package com.jarabaimport.levantamiento.ui.tank

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View

/** Panel de firma: dibuja con el dedo y exporta a PNG. */
class SignatureView(context: Context) : View(context) {

    private val paths = mutableListOf<Path>()
    private var current: Path? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    var existing: Bitmap? = null
        set(value) { field = value; invalidate() }
    var onChanged: (() -> Unit)? = null

    val hasDrawing: Boolean get() = paths.isNotEmpty()

    fun clear() {
        paths.clear(); current = null; invalidate(); onChanged?.invoke()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true) // evita que el ScrollView se desplace
                current = Path().apply { moveTo(e.x, e.y) }.also { paths.add(it) }
            }
            MotionEvent.ACTION_MOVE -> current?.lineTo(e.x, e.y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                current = null
                parent?.requestDisallowInterceptTouchEvent(false)
                onChanged?.invoke()
            }
        }
        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        val bg = existing
        if (paths.isEmpty() && bg != null) {
            val s = minOf(width.toFloat() / bg.width, height.toFloat() / bg.height)
            val w = bg.width * s; val h = bg.height * s
            val l = (width - w) / 2; val t = (height - h) / 2
            canvas.drawBitmap(bg, null, android.graphics.RectF(l, t, l + w, t + h), null)
        }
        paths.forEach { canvas.drawPath(it, paint) }
    }

    fun toBitmap(): Bitmap {
        val b = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.drawColor(Color.WHITE)
        paths.forEach { c.drawPath(it, paint) }
        return b
    }
}
