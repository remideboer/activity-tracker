package com.remideboer.freeactive.services

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import androidx.core.app.ActivityCompat
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
private const val MINIMAL_DISPLACEMENT = 10.0 // in meters

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
            .setContentTitle(resources.getString(R.string.notification_tracker_title_tracking_ongoing))
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
            // check if is paused or stopped to update gets not posted a final time after
            // remove and restarting te update cycle again
            if(ActivityTracker.isPaused().not()){
                updateNotification(getActivityTrackingText())
                handler.postDelayed(notificationUpdater, DELAY_INTERVAL)
            }
        }
    }

    private val wakeLock: WakeLock by lazy {
        (getSystemService(POWER_SERVICE) as PowerManager).
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
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
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener {
            lastTrackedLocation = LatLng(it.latitude, it.longitude)
        }
        // newer Android API uses different approach to keep the service alive android:foregroundServiceType="location"
        acquireWakeLock()

        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            when (it.action) {
                ACTION_PAUSE -> {
                    ActivityTracker.pause()
                    handler.removeCallbacks(notificationUpdater)
                    releaseWakeLock()
                    stopLocationUpdates()
                    // show paused text
                    updateNotification(
                        getActivityTrackingText(),
                        resources.getString(R.string.notification_tracker_title_tracking_paused) )
                }
                ACTION_RESUME -> {
                    ActivityTracker.resume()
                    handler.postDelayed(notificationUpdater, DELAY_INTERVAL)
                    acquireWakeLock()
                    startLocationUpdates()
                    updateNotification(getActivityTrackingText())
                }
                ACTION_STOP -> {
                    ActivityTracker.stop()
                    handler.removeCallbacks(notificationUpdater)
                    releaseWakeLock()
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

    private fun updateNotification(text: String, title: String = resources.getString(R.string.notification_tracker_title_tracking_ongoing)) {
        notificationBuilder.setContentTitle(title)
        notificationBuilder.setContentText(text)
        notificationBuilder.setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(text)
        )
        notificationManager.notify(NOTIFICATION_CODE, notificationBuilder.build())
    }

    private fun getActivityTrackingText(): String {
        return """
                Duur: ${DurationFormatUtils.formatDuration(
            ActivityTracker.getDuration().toMillis(),
            "H:mm:ss",
            true
        )}
                Afstand: %.0f meter
                Gemiddelde snelheid: %.0f km/u
            """.trimIndent()
            .format(ActivityTracker.getDistance(), ActivityTracker.getAverageSpeedKMPH())
    }

    override fun onDestroy() {
        ActivityTracker.reset()
        handler.removeCallbacks(notificationUpdater)
        stopLocationUpdates()
        releaseWakeLock()
    }

    private fun acquireWakeLock() {
        wakeLock.let {
            if(!it.isHeld){
                it.acquire()
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock.let {
            if (it.isHeld){
                it.release()
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
