package com.remideboer.freeactive.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.remideboer.freeactive.App
import com.remideboer.freeactive.R
import org.threeten.bp.Instant


private const val NOTIFICATION_CODE = 123

private const val ACTION_STOP = "STOP"
private const val ACTION_PAUSE = "PAUSE"
private const val ACTION_RESUME = "RESUME"

/**
 * Tracks current ongoing activity
 */
class ActivityTrackingForegroundService : Service() {

    private val TAG = ActivityTrackingForegroundService::class.java.simpleName
    private val notificationManager: NotificationManager by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val notificationBuilder by lazy(LazyThreadSafetyMode.NONE) {
        NotificationCompat.Builder(this, App.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(resources.getString(R.string.notification_tracker_title))
            .setSmallIcon(R.drawable.ic_icon_eye)
            .setOngoing(true) // so it can't be dismissed by the user
            // PRIORITY_MAX shows expanded notificationBuilder to show action button (add setWhen(0) for older versions if needed)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            // icons don't show using icons here, only needed for older version. Use custom layout for icons
            .addAction(
                R.drawable.ic_icon_stop,
                resources.getString(R.string.notification_stop),
                createPendingIntent(intent.apply { action = ACTION_STOP })
            )
            .addAction(
                R.drawable.ic_icon_pause,
                resources.getString(R.string.notification_pause),
                createPendingIntent(intent.apply { action = ACTION_PAUSE })
            )
            .addAction(
                R.drawable.ic_icon_play,
                resources.getString(R.string.notification_resume),
                createPendingIntent(intent.apply { action = ACTION_RESUME })
            )
    }
    private val intent by lazy(LazyThreadSafetyMode.NONE) {
        Intent(this, ActivityTrackingForegroundService::class.java)
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    private fun createPendingIntent(intent: Intent): PendingIntent {
        return PendingIntent.getService(
            this, 0,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            when (it.action) {
                ACTION_PAUSE -> {
                    val text = "Paused at ${Instant.now()}"
                    updateNotification(text)
                }
                ACTION_RESUME -> {
                    val text = "Resumed at ${Instant.now()}"
                    updateNotification(text)
                }
                ACTION_STOP -> {
                    stopSelf()
                }
            }
        }

        // create notificationBuilder
        startForeground(NOTIFICATION_CODE, notificationBuilder.build())

        // start up activity tracking

        return START_STICKY
    }

    private fun updateNotification(text: String) {
        notificationBuilder.setContentText(text)
        notificationBuilder.setStyle(
            NotificationCompat.BigTextStyle()
            .bigText(text))
        notificationManager.notify(NOTIFICATION_CODE, notificationBuilder.build())
    }
}
