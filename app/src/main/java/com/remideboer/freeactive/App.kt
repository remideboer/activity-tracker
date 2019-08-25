package com.remideboer.freeactive

import android.annotation.SuppressLint
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.jakewharton.threetenabp.AndroidThreeTen
import org.jetbrains.anko.doFromSdk



class App : Application() {

    private val TAG = App::class.java.simpleName
    private val notificationChannelDisplayName = "Free Active Tracking"

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "foregroundServiceNotificationChannel"
    }

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this);
        createNotificationChannels()
    }

    @SuppressLint("NewApi")
    private fun createNotificationChannels() {
        doFromSdk(Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                notificationChannelDisplayName,
                NotificationManager.IMPORTANCE_LOW
            ).apply { lockscreenVisibility = Notification.VISIBILITY_PUBLIC }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}