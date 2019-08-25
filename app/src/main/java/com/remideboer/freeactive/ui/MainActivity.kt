package com.remideboer.freeactive.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.core.content.ContextCompat
import com.remideboer.freeactive.R
import com.remideboer.freeactive.services.ActivityTrackingForegroundService

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        toolbar.setLogo(R.drawable.logo_running)

        fab.setOnClickListener { view ->

            ContextCompat.startForegroundService(this, Intent(this, ActivityTrackingForegroundService::class.java))
        }
    }
}
