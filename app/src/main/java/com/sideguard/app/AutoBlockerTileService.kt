package com.sideguard.app

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

/**
 * A Quick Settings tile that jumps straight to the Auto Blocker screen.
 * Swipe down → tap the tile → you're on Auto Blocker. No opening the app,
 * no digging through Settings.
 *
 * It opens the screen; it cannot flip the toggle (that needs a Samsung-signed
 * / privileged permission), so you tap the switch once there.
 */
class AutoBlockerTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val intent = Intent("com.samsung.android.rampart.action.MAIN_SETTING_ACTIVITY").apply {
            setComponent(
                ComponentName(
                    "com.samsung.android.rampart",
                    "com.samsung.android.rampart.ui.MainSettingActivity"
                )
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse34(intent)
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun startActivityAndCollapse34(intent: Intent) {
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        startActivityAndCollapse(pi)
    }
}
