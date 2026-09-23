package com.jarabaimport.levantamiento

import android.content.Context
import com.jarabaimport.levantamiento.data.db.AppDatabase
import com.jarabaimport.levantamiento.data.repository.ClientRepository
import com.jarabaimport.levantamiento.data.repository.FileStore
import com.jarabaimport.levantamiento.data.repository.PhotoRepository
import com.jarabaimport.levantamiento.data.repository.SettingsRepository
import com.jarabaimport.levantamiento.data.repository.TankRepository
import com.jarabaimport.levantamiento.export.ExportManager

/** Inyección de dependencias manual (sencilla, sin frameworks). */
class AppContainer(context: Context, db: AppDatabase = AppDatabase.build(context)) {
    val appContext: Context = context.applicationContext
    val database = db
    val files = FileStore(appContext)
    val settings = SettingsRepository(appContext)
    val clients = ClientRepository(database, files)
    val tanks = TankRepository(database, files)
    val photos = PhotoRepository(database, files)
    val exports = ExportManager(appContext, database, files)

    /** Para mostrar "Hay un levantamiento sin terminar" solo una vez por arranque. */
    var pendingPromptChecked = false
}
