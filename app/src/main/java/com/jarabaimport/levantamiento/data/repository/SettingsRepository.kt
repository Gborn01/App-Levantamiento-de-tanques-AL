package com.jarabaimport.levantamiento.data.repository

import android.content.Context

/** Preferencias simples (SharedPreferences). */
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("levantamiento_prefs", Context.MODE_PRIVATE)

    /** Tanque cuyo asistente quedó abierto (para "Hay un levantamiento sin terminar"). -1 = ninguno. */
    var pendingTankId: Long
        get() = prefs.getLong(KEY_PENDING_TANK, -1L)
        set(value) = prefs.edit().putLong(KEY_PENDING_TANK, value).apply()

    var pendingStep: Int
        get() = prefs.getInt(KEY_PENDING_STEP, 0)
        set(value) = prefs.edit().putInt(KEY_PENDING_STEP, value).apply()

    fun clearPending() = prefs.edit().remove(KEY_PENDING_TANK).remove(KEY_PENDING_STEP).apply()

    /** Último técnico usado, para prellenar. */
    var lastTechnician: String
        get() = prefs.getString(KEY_TECH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TECH, value).apply()

    var sampleDataSeeded: Boolean
        get() = prefs.getBoolean(KEY_SEEDED, false)
        set(value) = prefs.edit().putBoolean(KEY_SEEDED, value).apply()

    private companion object {
        const val KEY_PENDING_TANK = "pending_tank_id"
        const val KEY_PENDING_STEP = "pending_step"
        const val KEY_TECH = "last_technician"
        const val KEY_SEEDED = "sample_seeded"
    }
}
