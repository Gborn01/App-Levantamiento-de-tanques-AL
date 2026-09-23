package com.jarabaimport.levantamiento.ui

import android.app.Activity
import android.content.Intent
import com.jarabaimport.levantamiento.ui.clients.ClientDetailActivity
import com.jarabaimport.levantamiento.ui.clients.ClientFormActivity
import com.jarabaimport.levantamiento.ui.clients.ClientListActivity
import com.jarabaimport.levantamiento.ui.home.RecentActivity
import com.jarabaimport.levantamiento.ui.tank.TankSummaryActivity
import com.jarabaimport.levantamiento.ui.tank.TankWizardActivity

/** Navegación centralizada entre pantallas. */
object Nav {
    const val EXTRA_CLIENT = "clientId"
    const val EXTRA_TANK = "tankId"
    const val EXTRA_STEP = "step"

    fun clients(a: Activity) = a.startActivity(Intent(a, ClientListActivity::class.java))
    fun recent(a: Activity) = a.startActivity(Intent(a, RecentActivity::class.java))
    fun client(a: Activity, id: Long) = a.startActivity(Intent(a, ClientDetailActivity::class.java).putExtra(EXTRA_CLIENT, id))
    fun clientForm(a: Activity, id: Long = 0) = a.startActivity(Intent(a, ClientFormActivity::class.java).putExtra(EXTRA_CLIENT, id))
    /** Desde el asistente: vuelve al resumen existente (si lo hay) en vez de apilar otro. */
    fun summaryAfterWizard(a: Activity, tankId: Long) = a.startActivity(
        Intent(a, TankSummaryActivity::class.java).putExtra(EXTRA_TANK, tankId)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    )
    fun summary(a: Activity, tankId: Long) = a.startActivity(Intent(a, TankSummaryActivity::class.java).putExtra(EXTRA_TANK, tankId))
    fun wizard(a: Activity, clientId: Long, tankId: Long = 0, step: Int = 0) = a.startActivity(
        Intent(a, TankWizardActivity::class.java).putExtra(EXTRA_CLIENT, clientId).putExtra(EXTRA_TANK, tankId).putExtra(EXTRA_STEP, step)
    )
}
