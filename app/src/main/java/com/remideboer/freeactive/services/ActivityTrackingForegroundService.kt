package com.remideboer.freeactive.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.remideboer.freeactive.App
import com.remideboer.freeactive.R
import com.remideboer.freeactive.services.tracking.ActivityTracker
import com.remideboer.freeactive.ui.MainActivity
import org.apache.commons.lang3.time.DurationFormatUtils


private const val NOTIFICATION_CODE = 123

private const val ACTION_STOP = "STOP"
private const val ACTION_PAUSE = "PAUSE"
private const val ACTION_RESUME = "RESUME"

private const val DELAY_INTERVAL = 1000L

private val MINIMAL_DISPLACEMENT = 10.0 // in meters

/**
 * Tracks current ongoing activity
 */
class ActivityTrackingForegroundService : Service() {

    private val TAG = ActivityTrackingForegroundService::class.java.simpleName
    private val fusedLocationClient: FusedLocationProviderClient by lazy(LazyThreadSafetyMode.NONE) {
        LocationServices.getFusedLocationProviderClient(this)
    }
    private val notificationManager: NotificationManager by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val notificationBuilder by lazy(LazyThreadSafetyMode.NONE) {
        NotificationCompat.Builder(this, App.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(resources.getString(R.string.notification_tracker_title))
            .setContentIntent(mainIntent)
            .setSmallIcon(R.drawable.ic_icon_eye)
            .setOngoing(true) // so it can't be dismissed by the user
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // PRIORITY_MAX shows expanded notificationBuilder to show action button (add setWhen(0) for older versions if needed)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            // icons don't show using icons here, only needed for older version. Use custom layout for icons
            .addAction(
                R.drawable.ic_icon_stop,
                resources.getString(R.string.notification_stop),
                createPendingIntent(serviceIntent.apply { action = ACTION_STOP })
            )
            .addAction(
                R.drawable.ic_icon_pause,
                resources.getString(R.string.notification_pause),
                createPendingIntent(serviceIntent.apply { action = ACTION_PAUSE })
            )
            .addAction(
                R.drawable.ic_icon_play,
                resources.getString(R.string.notification_resume),
                createPendingIntent(serviceIntent.apply { action = ACTION_RESUME })
            )
    }
    private val mainIntent by lazy(LazyThreadSafetyMode.NONE) {
        TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(Intent(applicationContext, MainActivity::class.java))
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
    private val serviceIntent by lazy(LazyThreadSafetyMode.NONE) {
        Intent(this, ActivityTrackingForegroundService::class.java)
    }
    private val handler by lazy { Handler() }

    private val notificationUpdater: Runnable by lazy {
        Runnable {
            val text = """
            Duur: ${DurationFormatUtils.formatDuration(
                ActivityTracker.getDuration().toMillis(),
                "H:mm:ss",
                true
            )}
            Afstand: %.0f meter
            Gemiddelde snelheid: %.0f km/u
        """.trimIndent().format(ActivityTracker.getDistance(), ActivityTracker.getAverageSpeedKMPH())
            updateNotification(text)
            handler.postDelayed(notificationUpdater, DELAY_INTERVAL)
        }
    }

    private lateinit var lastTrackedLocation: LatLng
    private val locationRequest by lazy(LazyThreadSafetyMode.NONE) {
        LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private val locationCallback by lazy {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.let {

                    // check if minimal displacement is reached to update
                    val newLatLng = LatLng(it.lastLocation.latitude, it.lastLocation.longitude)

                    if (SphericalUtil.computeDistanceBetween(
                            newLatLng,
                            lastTrackedLocation
                        ) > MINIMAL_DISPLACEMENT
                    ) {
                        lastTrackedLocation = newLatLng
                        if (ActivityTracker.isTracking()) {
                            Log.d(TAG, "adding latln:${newLatLng} to route")
                            ActivityTracker.addPosition(newLatLng)
                        }
                    }
                }
            }
        }
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

    override fun onCreate() {
        // create notificationBuilder
        startForeground(NOTIFICATION_CODE, notificationBuilder.build())

        // starting updates as soon as possible to have a decent location
        fusedLocationClient.lastLocation.addOnSuccessListener {
            lastTrackedLocation = LatLng(it.latitude, it.longitude)
        }

        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            when (it.action) {
                ACTION_PAUSE -> {
                    ActivityTracker.pause()
                    handler.removeCallbacks(notificationUpdater)
                    stopLocationUpdates()
                }
                ACTION_RESUME -> {
                    ActivityTracker.resume()
                    handler.postDelayed(notificationUpdater, DELAY_INTERVAL)
                    startLocationUpdates()
                }
                ACTION_STOP -> {
                    ActivityTracker.stop()
                    handler.removeCallbacks(notificationUpdater)
                    stopLocationUpdates()
                    stopForeground(true)
                    stopSelf()
                }
                else -> Log.d(TAG, "default action")
            }
        }

        // start up activity tracking
        if (ActivityTracker.isTracking().not()) {
            ActivityTracker.start()
        }

        handler.postDelayed(notificationUpdater, DELAY_INTERVAL)

        return START_STICKY
    }

    private fun updateNotification(text: String) {
        notificationBuilder.setContentText(text)
        notificationBuilder.setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(text)
        )
        notificationManager.notify(NOTIFICATION_CODE, notificationBuilder.build())
    }

    override fun onDestroy() {
        ActivityTracker.reset()
        handler.removeCallbacks(notificationUpdater)
        stopLocationUpdates()
    }

    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
