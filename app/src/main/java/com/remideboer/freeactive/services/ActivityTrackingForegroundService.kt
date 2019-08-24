package com.remideboer.freeactive.services

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

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val notificationBuilder by lazy(LazyThreadSafetyMode.NONE) {
        NotificationCompat.Builder(this, App.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Free Active is Tracking")
            .setSmallIcon(R.drawable.ic_icon_eye)
            .setOngoing(true)
            // PRIORITY_MAX shows expanded notificationBuilder to show action button (add setWhen(0) for older versions if needed)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            // icons don't show using icons here, only needed for older version. Use custom layout for icons
            .addAction(
                R.drawable.ic_icon_stop,
                resources.getString(R.string.notification_stop),
                stopPendingIntent
            )
            .addAction(
                R.drawable.ic_icon_pause,
                resources.getString(R.string.notification_pause),
                pausePendingIntent
            )
            .addAction(
                R.drawable.ic_icon_play,
                resources.getString(R.string.notification_resume),
                resumePendingIntent
            )
    }
    private val stopPendingIntent by lazy(LazyThreadSafetyMode.NONE) {
        PendingIntent.getService(
            this, 0,
            stopIntent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
    }
    private val pausePendingIntent by lazy(LazyThreadSafetyMode.NONE) {
        PendingIntent.getService(
            this, 0,
            pauseIntent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
    }
    private val resumePendingIntent by lazy(LazyThreadSafetyMode.NONE) {
        PendingIntent.getService(
            this, 0,
            resumeIntent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
    }
    private val stopIntent by lazy(LazyThreadSafetyMode.NONE) {
        Intent(this, ActivityTrackingForegroundService::class.java)
            .apply { action = ACTION_STOP }
    }
    private val pauseIntent by lazy(LazyThreadSafetyMode.NONE) {
        Intent(this, ActivityTrackingForegroundService::class.java)
            .apply { action = ACTION_PAUSE }
    }
    private val resumeIntent by lazy(LazyThreadSafetyMode.NONE) {
        Intent(this, ActivityTrackingForegroundService::class.java)
            .apply { action = ACTION_RESUME }
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            when (it.action) {
                ACTION_PAUSE -> {
                    notificationBuilder.setContentText("Paused at ${Instant.now()}")
                    notificationManager.notify(NOTIFICATION_CODE, notificationBuilder.build())
                }
                ACTION_RESUME -> {
                    notificationBuilder.setContentText("Resumed at ${Instant.now()}")
                    notificationManager.notify(NOTIFICATION_CODE, notificationBuilder.build())
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
}
