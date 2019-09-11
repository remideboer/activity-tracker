package com.remideboer.freeactive.services.tracking

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.remideboer.freeactive.services.tracking.ActivityTracker.StateChangeListener
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.threeten.bp.Duration
import org.threeten.bp.Instant

class ActivityTrackerTest {

    @Rule
    @JvmField
    val expectedExceptioneption = ExpectedException.none()

    @After
    fun cleanUp() {
        ActivityTracker.reset()
    }

    @Test
    fun getStartTime() {
        ActivityTracker.start()

        val startTime = Instant.now()
        val result: Instant = ActivityTracker.getStartTime()
        Thread.sleep(5000)
        // verify seconds, nanos will always be off
        assertThat(result.epochSecond, `is`(startTime.epochSecond))

    }

    @Test
    fun getDuration() {
        val seconds = 5L
        ActivityTracker.start()

        Thread.sleep(seconds * 1000)
        val sample01: Duration = ActivityTracker.getDuration()

        Thread.sleep(seconds * 1000)
        val sample02: Duration = ActivityTracker.getDuration()

        // verify seconds, nanos will always be off
        assertThat(sample01.seconds, `is`(seconds))
        assertThat(sample02.seconds, `is`(seconds * 2))
    }

    @Test
    fun stopStopsTrackingAndKeepsGivingSameDurationAfterCall() {
        val seconds = 10L
        ActivityTracker.start()

        Thread.sleep(seconds * 1000)

        ActivityTracker.stop()

        Thread.sleep(seconds * 1000)

        val durationAfterStop: Duration = ActivityTracker.getDuration()

        // verify seconds, nanos will always be off
        assertThat(durationAfterStop.seconds, `is`(seconds))
    }

    @Test
    fun reset() {

        expectedExceptioneption.expect(IllegalStateException::class.java)

        ActivityTracker.reset()
    }


    @Test
    fun resetValuesAreSetToZero() {

        ActivityTracker.start()

        Thread.sleep(3000)

        ActivityTracker.reset()

        assertThat(ActivityTracker.getDuration(), `is`(Duration.ZERO))
        assertThat(ActivityTracker.getReadOnlyRoute().size, `is`(0))
        assertNull(ActivityTracker.getEndTimestamp())

        expectedExceptioneption.expect(java.lang.IllegalStateException::class.java)
        expectedExceptioneption.expectMessage("Stopwatch has not been started")
        ActivityTracker.getStartTime()
    }

    @Test
    fun pauseStopsDuration() {
        val seconds = 2L
        ActivityTracker.start()
        Thread.sleep(seconds * 1000)

        ActivityTracker.pause()

        Thread.sleep(seconds * 1000)

        val durationAfterPause: Duration = ActivityTracker.getDuration()

        // verify seconds, nanos will always be off
        assertThat(durationAfterPause.seconds, `is`(seconds))
    }

    @Test
    fun resumeContinuesDurationAfterPause() {
        val seconds = 2L
        ActivityTracker.start()
        Thread.sleep(seconds * 1000)

        ActivityTracker.pause()

        Thread.sleep(seconds * 1000)

        val durationAfterPause: Duration = ActivityTracker.getDuration()

        ActivityTracker.resume()
        Thread.sleep(seconds * 1000)

        val durationAfterResume: Duration = ActivityTracker.getDuration()

        // verify seconds, nanos will always be off
        assertThat(durationAfterPause.seconds, `is`(seconds))
        assertThat(durationAfterResume.seconds, greaterThan(seconds))
    }

    @Test
    fun getEndTimestampGivesUTCWhenStopped() {
        val seconds = 2L
        ActivityTracker.start()
        Thread.sleep(seconds * 1000)

        val expected = Instant.now()
        ActivityTracker.stop()

        // wait a bit more
        Thread.sleep(seconds * 1000)
        val endTime: Instant? = ActivityTracker.getEndTimestamp()

        assertThat(endTime!!.epochSecond, `is`(expected.epochSecond))

    }

    @Test
    fun startUpdatesListeners() {
        var expected01 = 3
        var expected02 = 5
        var listener01VarToBeUpdated = 1 // use this variable to check listener is called
        var listener02VarToBeUpdated = 1
        val changeListener: StateChangeListener = object : StateChangeListener {
            override fun onStart() {
                listener01VarToBeUpdated = expected01
            }

            override fun onStop() {}
            override fun onPause() {}
            override fun onResume() {}
        }

        val changeListener2: StateChangeListener = object : StateChangeListener {
            override fun onStart() {
                listener02VarToBeUpdated = expected02
            }

            override fun onStop() {}
            override fun onPause() {}
            override fun onResume() {}
        }

        ActivityTracker.addStateChangeListener(changeListener)
        ActivityTracker.addStateChangeListener(changeListener2)
        ActivityTracker.start()

        assertThat(listener01VarToBeUpdated, `is`(expected01))
        assertThat(listener02VarToBeUpdated, `is`(expected02))
    }

    @Test
    fun stopUpdatesListeners() {
        var expected01 = 3
        var expected02 = 5
        var listener01VarToBeUpdated = 1 // use this variable to check listener is called
        var listener02VarToBeUpdated = 1
        val changeListener: StateChangeListener = object : StateChangeListener {
            override fun onStart() {}
            override fun onStop() {
                listener01VarToBeUpdated = expected01
            }

            override fun onPause() {}
            override fun onResume() {}
        }

        val changeListener2: StateChangeListener = object : StateChangeListener {
            override fun onStart() {}
            override fun onStop() {
                listener02VarToBeUpdated = expected02
            }

            override fun onPause() {}
            override fun onResume() {}
        }

        ActivityTracker.addStateChangeListener(changeListener)
        ActivityTracker.addStateChangeListener(changeListener2)
        ActivityTracker.start()
        ActivityTracker.stop()

        assertThat(listener01VarToBeUpdated, `is`(expected01))
        assertThat(listener02VarToBeUpdated, `is`(expected02))
    }

    @Test
    fun pauseUpdatesListeners() {
        var expected01 = 3
        var expected02 = 5
        var listener01VarToBeUpdated = 1 // use this variable to check listener is called
        var listener02VarToBeUpdated = 1
        val changeListener: StateChangeListener = object : StateChangeListener {
            override fun onStart() {}
            override fun onStop() {}
            override fun onPause() {
                listener01VarToBeUpdated = expected01
            }

            override fun onResume() {}
        }

        val changeListener2: StateChangeListener = object : StateChangeListener {
            override fun onStart() {}
            override fun onStop() {}
            override fun onPause() {
                listener02VarToBeUpdated = expected02
            }

            override fun onResume() {}
        }

        ActivityTracker.addStateChangeListener(changeListener)
        ActivityTracker.addStateChangeListener(changeListener2)
        ActivityTracker.start()
        ActivityTracker.pause()

        assertThat(listener01VarToBeUpdated, `is`(expected01))
        assertThat(listener02VarToBeUpdated, `is`(expected02))
    }

    @Test
    fun resumeUpdatesListeners() {
        var expected01 = 3
        var expected02 = 5
        var listener01VarToBeUpdated = 1 // use this variable to check listener is called
        var listener02VarToBeUpdated = 1
        val changeListener: StateChangeListener = object : StateChangeListener {
            override fun onStart() {}
            override fun onStop() {}
            override fun onPause() {}
            override fun onResume() {
                listener01VarToBeUpdated = expected01
            }
        }

        val changeListener2: StateChangeListener = object : StateChangeListener {
            override fun onStart() {}
            override fun onStop() {}
            override fun onPause() {}
            override fun onResume() {
                listener02VarToBeUpdated = expected02
            }
        }

        ActivityTracker.addStateChangeListener(changeListener)
        ActivityTracker.addStateChangeListener(changeListener2)
        ActivityTracker.start()
        ActivityTracker.pause()
        ActivityTracker.resume()

        assertThat(listener01VarToBeUpdated, `is`(expected01))
        assertThat(listener02VarToBeUpdated, `is`(expected02))
    }

    @Test
    fun removeStateChangeListener() {
        var updateWith = 3
        var result = 5 // use this variable to check listener is called

        val stateChangeListener: StateChangeListener = object : StateChangeListener {
            override fun onStart() {
                result = updateWith
            } // must not be called

            override fun onStop() {}
            override fun onPause() {}
            override fun onResume() {}
        }

        ActivityTracker.addStateChangeListener(stateChangeListener)
        ActivityTracker.removeStateChangeListener(stateChangeListener) // now this var should remain unchanged
        ActivityTracker.start()

        assertThat(result, not(updateWith))
    }

    @Test
    fun addLatLng() {
        // test adding latLng
        // retrieve route as list
        val positions = listOf(
            LatLng(51.606077, 5.555630),
            LatLng(52.606077, 5.655630),
            LatLng(53.606077, 5.755630)
        )
        ActivityTracker.addPosition(positions[0])
        ActivityTracker.addPosition(positions[1])
        ActivityTracker.addPosition(positions[2])

        val route: List<LatLng> = ActivityTracker.getReadOnlyRoute()

        assertThat(route[0], `is`(positions[0]))
        assertThat(route[1], `is`(positions[1]))
        assertThat(route[2], `is`(positions[2]))

    }

    @Test
    fun start_already_started_resets_data_start_time() {
        ActivityTracker.start()
        val start = ActivityTracker.getStartTime()
        Thread.sleep(2000)
        ActivityTracker.stop()

        ActivityTracker.start()
        val start2 = ActivityTracker.getStartTime()
        ActivityTracker.stop()

        assertThat(start2.epochSecond, not(`is`(start.epochSecond)))
    }

    @Test
    fun start_already_started_resets_data_duration() {
        // have the second duration be smaller then first
        // that would be impossible without a reset
        ActivityTracker.start()
        Thread.sleep(2000)
        ActivityTracker.stop()
        val duration1 = ActivityTracker.getDuration()

        ActivityTracker.start()
        Thread.sleep(1000)
        ActivityTracker.stop()
        val duration2 = ActivityTracker.getDuration()

        assertThat(duration2.seconds, lessThan(duration1.seconds))
    }

    @Test
    fun getTrackedActivity_started_and_stopped_gives_correct_data() {
        val routeList = listOf(
            LatLng(52.370216, 4.895168), LatLng(52.090736, 5.121420),
            LatLng(52.957378, 4.758500), LatLng(52.350784, 5.264702)
        )
        val encodedRoute = PolyUtil.encode(routeList)
        val seconds = 2L
        val sleep = seconds * 1000L

        val startTime = Instant.now().epochSecond
        ActivityTracker.start()
        routeList.forEach {
            ActivityTracker.addPosition(it)
        }

        Thread.sleep(sleep)
        ActivityTracker.stop()
        val endTime = Instant.now().epochSecond

        val result = ActivityTracker.getTrackedActivity()

        assertThat(result.startInstant?.epochSecond, `is`(startTime))
        assertThat(result.endInstant?.epochSecond, `is`(endTime))
        assertThat(result.duration?.seconds, `is`(seconds))
        assertThat(result.route, `is`(encodedRoute))
    }

    @Test
    fun getTrackedActivity_started_and_not_stopped_throws_illegalstate_exception() {
        expectedExceptioneption.expect(IllegalStateException::class.java)
        expectedExceptioneption.expectMessage("ActivityTracker is still running. A TrackedActivity can only be obtained from a stopped ActivityTracker. Call stop first")
        ActivityTracker.start()
        ActivityTracker.getTrackedActivity()

    }


}
