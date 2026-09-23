package com.jarabaimport.levantamiento

import android.app.Application
import com.jarabaimport.levantamiento.data.sample.SampleData
import com.jarabaimport.levantamiento.util.CrashReporter

class LevantamientoApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        container = AppContainer(this)
        // Datos de prueba (solo la primera vez). Si fallaran, la app debe abrir igualmente.
        try {
            SampleData.seedIfNeeded(container.settings, container.clients, container.tanks)
        } catch (e: Throwable) {
            CrashReporter.record(this, e, "datos de ejemplo")
        }
    }
}

/** Acceso al contenedor desde cualquier Activity. */
val android.app.Activity.app: AppContainer get() = (application as LevantamientoApp).container
