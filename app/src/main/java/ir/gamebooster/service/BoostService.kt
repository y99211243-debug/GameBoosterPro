package ir.gamebooster.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import ir.gamebooster.MainActivity
import ir.gamebooster.core.Shell

class BoostService : Service() {
    override fun onCreate() {
        super.onCreate()
        Shell.detect()
        startForeground(1001, notif("فعال"))
    }

    private fun notif(t: String): Notification {
        val ch = NotificationChannel("gb", "Game Booster", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, "gb")
            .setContentTitle("Game Booster Pro")
            .setContentText(t)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    override fun onBind(i: Intent?): IBinder? = null
}
