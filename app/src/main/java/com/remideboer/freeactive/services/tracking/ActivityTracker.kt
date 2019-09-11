package com.remideboer.freeactive.services.tracking

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import org.apache.commons.lang3.time.StopWatch
import org.threeten.bp.Duration
import org.threeten.bp.Instant

/**
 * Responsible for tracking activities.
 * Holds data and function to interact and change state of tracking
 * Partially a wrapper around Stopwatch but must also hold Route data and additional data
 * If state calls are needed, Stopwatch state could be forwarded or simplified
 */
object ActivityTracker {

    /**
     * Interface to register callback listeners for responding to Activity state change
     * possible states: started, paused, stopped
     * Use these functions call backs to respond to state changes
     */
    interface StateChangeListener {
        fun onStart()
        fun onStop()
        fun onPause()
        fun onResume()
    }

    private val route: MutableList<LatLng> by lazy(LazyThreadSafetyMode.NONE) { mutableListOf<LatLng>() }
    private val stateChangeListeners: MutableList<StateChangeListener> by lazy(LazyThreadSafetyMode.NONE) { mutableListOf<StateChangeListener>() }
    private var endTime: Instant? = null
    /**
     * Keeps track of timing data, except end time UTC
     */
    private val stopWatch by lazy { StopWatch() }

    fun getStartTime(): Instant {
        return Instant.ofEpochMilli(stopWatch.startTime)
    }

    fun getDuration(): Duration {
        return Duration.ofMillis(stopWatch.time)
    }

    fun start() {
        // starts activity tracking using Apache Stopwatch
        reset() // reset data
        stopWatch.start()
        // update listeners
        for (listener in stateChangeListeners) {
            listener.onStart()
        }
    }

    fun stop() {
        // stops everything
        // could build data object on stop
        if (stopWatch.isStarted) {
            stopWatch.stop()
            endTime = Instant.now()
            for (listener in stateChangeListeners) {
                listener.onStop()
            }
        }
    }

    fun pause() {
        // check state
        if (stopWatch.isSuspended.not()) {
            stopWatch.suspend()
            for (listener in stateChangeListeners) {
                listener.onPause()
            }
        }
    }

    fun resume() {
        if (stopWatch.isSuspended) {
            stopWatch.resume()
            for (listener in stateChangeListeners) {
                listener.onResume()
            }
        }
    }

    fun getEndTimestamp(): Instant? {
        return endTime
    }

    fun addStateChangeListener(changeListener: StateChangeListener) {
        stateChangeListeners.add(changeListener)
    }

    fun removeStateChangeListener(changeListener: StateChangeListener) {
        stateChangeListeners.remove(changeListener)
    }

    fun reset() {
        if (stopWatch.isStarted) {
            stopWatch.stop()
        }

        stopWatch.reset()
        endTime = null
        route.clear()
    }

    fun addPosition(position: LatLng) {
        route.add(position)
    }

    fun getReadOnlyRoute(): List<LatLng> {
        return route.toMutableList()
    }

    fun isTracking(): Boolean {
        return stopWatch.isStarted
    }

    fun isPaused(): Boolean {
        return stopWatch.isSuspended
    }

    /**
     * calculates length of the route in meters
     */
    fun getDistance(): Double {
        return SphericalUtil.computeLength(route)
    }

    /**
     * Calculates the current average speed in km/h based on route length
     * and active tracked time
     */
    fun getAverageSpeedKMPH(): Double {
        return averageSpeedMPS() * 3.6
    }

    private fun averageSpeedMPS() = getDistance() / getDuration().seconds

}
