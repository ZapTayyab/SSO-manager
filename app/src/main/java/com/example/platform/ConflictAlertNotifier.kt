package com.example.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

data class ConflictWarningEvent(
    val id: String,
    val title: String,
    val message: String,
    val overlapCount: Int = 0,
    val venueClashCount: Int = 0
)

/**
 * Displays immediate warning Toasts and Android system notifications
 * whenever an overlapping teaching time or venue booking conflict is detected.
 */
class ConflictAlertNotifier(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        createNotificationChannel()
    }

    fun notifyConflict(event: ConflictWarningEvent) {
        showWarningToast(event.message)
        postConflictNotification(event.title, event.message)
    }

    fun showWarningToast(message: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
        } else {
            mainHandler.post {
                Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun postConflictNotification(title: String, message: String) {
        if (!canPostNotifications()) return

        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentIntent = PendingIntent.getActivity(
            appContext,
            NOTIFICATION_REQUEST_CODE,
            launchIntent,
            pendingIntentFlags
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        try {
            NotificationManagerCompat.from(appContext).notify(CONFLICT_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked between check and notify; Toast still alerts the user.
        }
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(appContext).areNotificationsEnabled()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.notification_channel_conflicts_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = appContext.getString(R.string.notification_channel_conflicts_desc)
                enableVibration(true)
            }
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "sso_timetable_conflicts"
        const val CONFLICT_NOTIFICATION_ID = 4041
        private const val NOTIFICATION_REQUEST_CODE = 4042
    }
}
