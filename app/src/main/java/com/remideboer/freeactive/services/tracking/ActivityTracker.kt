package com.remideboer.freeactive.services.tracking

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

    private val stateChangeListeners: MutableList<StateChangeListener> by lazy(LazyThreadSafetyMode.NONE) { mutableListOf<StateChangeListener>() }
    private var endTime: Instant? = null
    private val stopWatch by lazy { StopWatch() }

    fun start() {
        // starts activity tracking using Apache Stopwatch
        stopWatch.start()
        // update listeners
        for (listener in stateChangeListeners){
            listener.onStart()
        }
    }

    fun getStartTime(): Instant {
        return Instant.ofEpochMilli(stopWatch.startTime)
    }

    fun getDuration(): Duration {
        return Duration.ofMillis(stopWatch.time)
    }

    fun stop() {
        // stops everything
        // could build data object on stop
        stopWatch.stop()
        endTime = Instant.now()
        for(listener in stateChangeListeners){
            listener.onStop()
        }
    }

    fun pause() {
        stopWatch.suspend()
        for(listener in stateChangeListeners){
            listener.onPause()
        }
    }

    fun resume() {
        stopWatch.resume()
        for(listener in stateChangeListeners){
            listener.onResume()
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
        if(stopWatch.isStarted){
            stopWatch.stop()
        }

        stopWatch.reset()
        endTime = null
    }

}