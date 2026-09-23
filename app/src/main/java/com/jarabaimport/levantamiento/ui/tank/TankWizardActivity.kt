package com.jarabaimport.levantamiento.ui.tank

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.jarabaimport.levantamiento.data.db.PhotoEntity
import com.jarabaimport.levantamiento.data.db.TankEntity
import com.jarabaimport.levantamiento.domain.Fmt
import com.jarabaimport.levantamiento.domain.TankValidator
import com.jarabaimport.levantamiento.domain.model.PhotoCategory
import com.jarabaimport.levantamiento.ui.BaseActivity
import com.jarabaimport.levantamiento.ui.Field
import com.jarabaimport.levantamiento.ui.Nav
import com.jarabaimport.levantamiento.ui.Palette
import com.jarabaimport.levantamiento.ui.dp
import com.jarabaimport.levantamiento.ui.dpf
import com.jarabaimport.levantamiento.ui.lpMatchWrap
import com.jarabaimport.levantamiento.ui.makeButton
import com.jarabaimport.levantamiento.ui.makeText
import com.jarabaimport.levantamiento.ui.rounded
import com.jarabaimport.levantamiento.ui.weighted
import com.jarabaimport.levantamiento.util.AppFileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Asistente de levantamiento en 8 pasos.
 *
 * AUTOGUARDADO: cada cambio programa un guardado (~0.7 s); también se guarda al cambiar de paso,
 * al salir y en onPause. Mientras el asistente está abierto se recuerda el tanque en preferencias:
 * si la app se cierra de forma inesperada, al volver aparece "Hay un levantamiento sin terminar".
 * Los valores numéricos inválidos nunca se guardan (se guardan como "no informado").
 */
class TankWizardActivity : BaseActivity() {

    var draft: TankEntity = TankEntity(clientId = 0)
        private set
    var clientName = ""
        private set
    var photos: List<PhotoEntity> = emptyList()
        private set
    var volumeInM3 = false
    val fields = mutableMapOf<String, Field>()

    private var step = 0
    private var errors: Map<String, String> = emptyMap()
    private var dirty = false
    private var lastSavedAt: Long? = null

    private lateinit var stepHeader: LinearLayout
    private val autosave = Runnable { saveNow() }

    // Foto en curso (se conserva si Android cierra la Activity mientras la cámara está abierta)
    private var pendingPhotoPath: String? = null
    private var pendingPhotoCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val clientId = intent.getLongExtra(Nav.EXTRA_CLIENT, 0L)
        val tankId = savedInstanceState?.getLong(Nav.EXTRA_TANK) ?: intent.getLongExtra(Nav.EXTRA_TANK, 0L)
        step = (savedInstanceState?.getInt(Nav.EXTRA_STEP) ?: intent.getIntExtra(Nav.EXTRA_STEP, 0)).coerceIn(0, WIZARD_STEPS.lastIndex)
        pendingPhotoPath = savedInstanceState?.getString("pendingPhotoPath")
        pendingPhotoCategory = savedInstanceState?.getString("pendingPhotoCategory")

        val loaded = if (tankId > 0) c.tanks.get(tankId) else null
        draft = loaded ?: TankEntity(
            clientId = clientId,
            code = c.tanks.suggestNextCode(clientId),
            technician = c.settings.lastTechnician,
        )
        clientName = c.clients.get(draft.clientId)?.name ?: ""
        if (loaded != null) {
            photos = c.photos.byTank(loaded.id)
            markPending()
        }

        setupScreen(title(), clientName)
        stepHeader = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setBackgroundColor(Palette.WHITE)
        }
        root.addView(stepHeader, 2, lpMatchWrap) // debajo de la barra superior y la línea de acento
        bottomBar.visibility = View.VISIBLE
        renderStep(scrollTop = true)
    }

    private fun title() = if (draft.id == 0L) "Nuevo tanque" else draft.code.ifBlank { "Editar tanque" }

    // ------------------------------------------------------------ render

    fun renderStep(scrollTop: Boolean = false) {
        fields.clear()
        renderHeader()
        renderBottom()
        rebuild(keepScroll = !scrollTop) { buildStep(step, StepEnv(this@TankWizardActivity, draft, errors)) }
    }

    private fun renderHeader() {
        stepHeader.removeAllViews()
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.weighted(makeText(this, "PASO ${step + 1} DE ${WIZARD_STEPS.size}", 14f, bold = true, color = Palette.ACCENT), 1f, 0)
        top.addView(makeText(this, lastSavedAt?.let { "✓ Guardado " + Fmt.dateTime(it).substringAfter(' ') } ?: "", 13f, bold = true, color = Palette.OK))
        stepHeader.addView(top)
        stepHeader.addView(makeText(this, WIZARD_STEPS[step], 22f, bold = true, color = Palette.NAVY).apply { setPadding(0, dp(2), 0, dp(8)) })
        val circles = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        WIZARD_STEPS.indices.forEach { i ->
            val active = i == step
            val done = i < step
            circles.addView(TextView(this).apply {
                text = "${i + 1}"
                gravity = Gravity.CENTER
                textSize = 16f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(if (active) Palette.WHITE else Palette.NAVY)
                background = rounded(if (active) Palette.NAVY else if (done) Palette.SKY else Palette.WHITE, dpf(22f),
                    if (active || done) Palette.NAVY else Palette.OUTLINE, dp(2))
                contentDescription = "Paso ${i + 1}: ${WIZARD_STEPS[i]}"
                setOnClickListener { goTo(i) }
            }, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginEnd = dp(6) })
        }
        stepHeader.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(circles) })
        stepHeader.addView(View(this).apply { setBackgroundColor(0xFFE1E5EA.toInt()) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply { topMargin = dp(10) })
        setTitles(title(), clientName)
    }

    private fun renderBottom() {
        bottomBar.removeAllViews()
        bottomBar.weighted(makeButton(this, "← Anterior", primary = false, enabled = step > 0) { back() }, 1f, 0)
        if (step < WIZARD_STEPS.lastIndex) bottomBar.weighted(makeButton(this, "Siguiente →") { next() })
        else bottomBar.weighted(makeButton(this, "✓ Finalizar") { finishSurvey() })
    }

    private fun refreshSavedLabel() {
        val top = stepHeader.getChildAt(0) as? LinearLayout ?: return
        (top.getChildAt(1) as? TextView)?.text = lastSavedAt?.let { "✓ Guardado " + Fmt.dateTime(it).substringAfter(' ') } ?: ""
    }

    // ------------------------------------------------------------ edición y guardado

    /** Aplica un cambio al borrador y programa el autoguardado. */
    fun update(f: (TankEntity) -> TankEntity, rerender: Boolean) {
        draft = f(draft)
        dirty = true
        if (errors.isNotEmpty()) {
            errors = TankValidator.errorsForStep(draft, step)
            fields.forEach { (k, fld) -> fld.setError(errors[k]) }
        }
        cancel(autosave)
        postDelayed(700, autosave)
        if (rerender) renderStep()
    }

    /** Guarda en la base de datos (no crea el registro hasta que haya nombre de tanque). */
    fun saveNow(): Long? {
        cancel(autosave)
        if (draft.name.isBlank()) return null
        if (!dirty && draft.id > 0) return draft.id
        val toStore = TankValidator.sanitizeForStorage(draft).copy(lastStep = step)
        val isNew = draft.id == 0L
        val id = c.tanks.save(toStore)
        val saved = c.tanks.get(id)
        draft = draft.copy(id = id, status = saved?.status ?: draft.status)
        dirty = false
        lastSavedAt = System.currentTimeMillis()
        markPending()
        if (isNew) setTitles(title(), clientName)
        if (::stepHeader.isInitialized) refreshSavedLabel()
        return id
    }

    private fun markPending() {
        c.settings.pendingTankId = draft.id
        c.settings.pendingStep = step
    }

    private fun showErrors(e: Map<String, String>) {
        errors = e
        fields.forEach { (k, fld) -> fld.setError(e[k]) }
        val first = fields.entries.firstOrNull { e.containsKey(it.key) }?.value
        first?.let { f ->
            scroll.post {
                val parentTop = (f.container.parent as? View)?.top ?: 0
                scroll.smoothScrollTo(0, (f.container.top + parentTop - dp(24)).coerceAtLeast(0))
            }
        }
    }

    private fun next() {
        val e = TankValidator.errorsForStep(draft, step)
        if (e.isNotEmpty()) {
            showErrors(e)
            toast(e.values.first(), true)
            return
        }
        dirty = true
        saveNow()
        errors = emptyMap()
        step++
        markPending()
        renderStep(scrollTop = true)
    }

    private fun back() {
        if (step == 0) return
        saveNow()
        errors = emptyMap()
        step--
        markPending()
        renderStep(scrollTop = true)
    }

    private fun goTo(target: Int) {
        if (target == step) return
        if (draft.name.isBlank()) {
            step = 0
            renderStep(scrollTop = true)
            showErrors(TankValidator.errorsForStep(draft, 0))
            toast("Primero indique el nombre del tanque.")
            return
        }
        dirty = true
        saveNow()
        errors = emptyMap()
        step = target
        markPending()
        renderStep(scrollTop = true)
    }

    private fun finishSurvey() {
        val all = TankValidator.validateAll(draft)
        if (all.isNotEmpty()) {
            val first = TankValidator.firstStepWithErrors(draft) ?: 0
            step = first
            errors = TankValidator.errorsForStep(draft, first)
            renderStep(scrollTop = true)
            toast("No se puede finalizar: revise el paso ${first + 1} (${WIZARD_STEPS[first]}).", true)
            return
        }
        dirty = true
        val id = saveNow() ?: return
        if (draft.technician.isNotBlank()) c.settings.lastTechnician = draft.technician
        c.settings.clearPending()
        Nav.summaryAfterWizard(this, id)
        finish()
    }

    /** Salida voluntaria: se guarda como borrador y no se mostrará el aviso al reabrir. */
    @Deprecated("API de plataforma")
    override fun onBackPressed() {
        saveNow()
        if (draft.technician.isNotBlank()) c.settings.lastTechnician = draft.technician
        c.settings.clearPending()
        if (draft.id > 0) toast("Levantamiento guardado.")
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    override fun onPause() {
        super.onPause()
        saveNow()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(Nav.EXTRA_TANK, draft.id)
        outState.putInt(Nav.EXTRA_STEP, step)
        pendingPhotoPath?.let { outState.putString("pendingPhotoPath", it) }
        pendingPhotoCategory?.let { outState.putString("pendingPhotoCategory", it) }
    }

    // ------------------------------------------------------------ fotos

    private fun ensureSaved(): Long? {
        val id = draft.id.takeIf { it > 0 } ?: run { dirty = true; saveNow() }
        if (id == null) toast("Indique primero el nombre del tanque (paso 1).", true)
        return id
    }

    fun takePhoto(cat: PhotoCategory) {
        ensureSaved() ?: return
        val f = File(c.files.cameraTempDir, "cap_${System.currentTimeMillis()}.jpg")
        val uri = AppFileProvider.uriFor(this, f)
        pendingPhotoPath = f.absolutePath
        pendingPhotoCategory = cat.code
        val i = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            clipData = ClipData.newRawUri("foto", uri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivityForResult(i, REQ_CAMERA)
        } catch (e: Exception) {
            toast("No se encontró una app de cámara.", true)
        }
    }

    fun pickPhoto(cat: PhotoCategory) {
        ensureSaved() ?: return
        pendingPhotoCategory = cat.code
        val i = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            startActivityForResult(Intent.createChooser(i, "Elegir foto"), REQ_GALLERY)
        } catch (e: Exception) {
            toast("No hay galería disponible.", true)
        }
    }

    override fun onUnhandledResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val cat = PhotoCategory.fromCode(pendingPhotoCategory)
        when (requestCode) {
            REQ_CAMERA -> {
                val f = pendingPhotoPath?.let { File(it) }
                pendingPhotoPath = null
                if (resultCode == RESULT_OK && f != null && f.exists() && f.length() > 0) importPhoto(cat, Uri.fromFile(f), f)
                else f?.delete()
            }
            REQ_GALLERY -> {
                val uri = data?.data
                if (resultCode == RESULT_OK && uri != null) importPhoto(cat, uri, null)
            }
        }
    }

    private fun importPhoto(cat: PhotoCategory, uri: Uri, temp: File?) {
        val tankId = draft.id.takeIf { it > 0 } ?: return
        toast("Guardando foto…")
        bg({ c.photos.addFromUri(applicationContext, tankId, cat, uri) }) { r ->
            temp?.delete()
            toast(if (r.getOrDefault(false)) "Foto guardada: ${cat.label}" else "No se pudo guardar la foto.")
            photos = c.photos.byTank(tankId)
            if (step == 6) renderStep()
        }
    }

    fun deletePhoto(p: PhotoEntity) {
        c.photos.delete(p)
        photos = c.photos.byTank(draft.id)
        renderStep()
    }

    // ------------------------------------------------------------ firma

    fun saveSignature(bitmap: Bitmap) {
        val id = ensureSaved() ?: return
        val file = c.files.signatureFile(id)
        try {
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } catch (e: Exception) {
            toast("No se pudo guardar la firma."); return
        }
        val old = draft.signaturePath
        update({ it.copy(signaturePath = file.absolutePath) }, rerender = false)
        saveNow()
        if (old.isNotBlank() && old != file.absolutePath) File(old).delete()
        toast("Firma guardada.")
        renderStep()
    }

    fun clearSignature() {
        val old = draft.signaturePath
        if (old.isBlank()) return
        update({ it.copy(signaturePath = "") }, rerender = false)
        saveNow()
        File(old).delete()
        renderStep()
    }

    companion object {
        private const val REQ_CAMERA = 501
        private const val REQ_GALLERY = 502
    }
}
