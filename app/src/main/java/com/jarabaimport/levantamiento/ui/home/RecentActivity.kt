package com.jarabaimport.levantamiento.ui.home

import android.os.Bundle
import com.jarabaimport.levantamiento.ui.BaseActivity
import com.jarabaimport.levantamiento.ui.Nav
import com.jarabaimport.levantamiento.ui.Palette
import com.jarabaimport.levantamiento.ui.clients.tankCard
import com.jarabaimport.levantamiento.ui.text

class RecentActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupScreen("Levantamientos recientes", "Ordenados por última modificación")
    }

    override fun onResume() {
        super.onResume()
        val items = c.tanks.recent(100)
        rebuild {
            if (items.isEmpty()) text("Aún no hay levantamientos.", color = Palette.MUTED)
            items.forEach { t -> tankCard(t, showClient = true) { Nav.summary(this@RecentActivity, t.id) } }
        }
    }
}
