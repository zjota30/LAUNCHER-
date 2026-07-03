package com.diamondrp.launcher

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.diamondrp.launcher.utils.Constants
import com.diamondrp.launcher.utils.ThemeManager

class DiamondRPApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ThemeManager.applySavedTheme(this)
        createDownloadNotificationChannel()
    }

    private fun createDownloadNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.DOWNLOAD_CHANNEL_ID,
                getString(R.string.app_name) + " - Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificações de progresso de download de atualizações"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
