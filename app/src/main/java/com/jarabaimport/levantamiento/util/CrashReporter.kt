package com.jarabaimport.levantamiento.util

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Guarda el último error inesperado en un archivo para mostrarlo (y poder enviarlo)
 * la próxima vez que se abra la app. No usa Internet.
 */
object CrashReporter {
    private fun file(ctx: Context) = File(ctx.filesDir, "last_crash.txt")

    fun install(ctx: Context) {
        val app = ctx.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            record(app, e, "hilo ${t.name}")
            previous?.uncaughtException(t, e)
        }
    }

    fun record(ctx: Context, e: Throwable, where: String) {
        try {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val info = "Levantamiento Tanques · error en $where\n" +
                "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}\n\n$sw"
            file(ctx).writeText(info)
        } catch (_: Throwable) {
        }
    }

    /** Devuelve y borra el último error guardado (o null). */
    fun takeLast(ctx: Context): String? {
        val f = file(ctx)
        if (!f.exists()) return null
        return try { f.readText() } catch (_: Throwable) { null } finally { f.delete() }
    }
}
