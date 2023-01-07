package com.remideboer.freeactive.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.core.content.ContextCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import com.remideboer.freeactive.R
import com.remideboer.freeactive.services.ActivityTrackingForegroundService

import kotlinx.android.synthetic.main.activity_main.*
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener
import org.jetbrains.anko.internals.AnkoInternals.createAnkoContext


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        toolbar.setLogo(R.drawable.logo_running)

        fab.setOnClickListener { view ->

            ContextCompat.startForegroundService(this, Intent(this, ActivityTrackingForegroundService::class.java))
        }

        askRuntimePermissions()
    }

    private fun askRuntimePermissions() {
        val dialogPermissionListener = DialogOnAnyDeniedMultiplePermissionsListener.Builder
            .withContext(this)
            .withTitle("Location Permission")
            .withMessage("Background Location permission is needed to keep track of your route when screen is locked")
            .withButtonText(android.R.string.ok)
            .build()

        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            .withListener(dialogPermissionListener)
            .check()
    }
}
