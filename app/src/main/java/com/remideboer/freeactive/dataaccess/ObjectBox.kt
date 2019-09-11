package com.remideboer.freeactive.dataaccess

import android.content.Context
import android.util.Log
import com.remideboer.freeactive.App
import com.remideboer.freeactive.BuildConfig
import com.remideboer.freeactive.entities.MyObjectBox
import io.objectbox.BoxStore
import io.objectbox.android.AndroidObjectBrowser

/**
 * Singleton to keep BoxStore reference.
 */
object ObjectBox {

    lateinit var boxStore: BoxStore
        private set

    fun init(context: Context) {
        boxStore = MyObjectBox.builder().androidContext(context.applicationContext).build()

        // to start be able to run the object browser
        if (BuildConfig.DEBUG) {
            Log.d("Free Active", "Using ObjectBox ${BoxStore.getVersion()} (${BoxStore.getVersionNative()})")
            AndroidObjectBrowser(boxStore).start(context.applicationContext)
        }
    }

}