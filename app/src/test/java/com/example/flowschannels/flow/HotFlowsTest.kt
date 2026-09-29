package com.example.flowschannels.flow

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StateFlowTest {

    @Test
    fun stateFlow_has_initial_value() = runTest {
        val state = MutableStateFlow(0)
        assertEquals(0, state.value)
        state.test {
            assertEquals(0, awaitItem())
            state.update { it + 1 }
            assertEquals(1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun equal_values_are_conflated() = runTest {
        val state = MutableStateFlow("Ada")
        state.test {
            assertEquals("Ada", awaitItem())
            state.value = "Ada"
            expectNoEvents()
            state.value = "Grace"
            assertEquals("Grace", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

class SharedFlowTest {

    @Test
    fun sharedFlow_without_replay_does_not_emit_past_values() = runTest {
        val events = MutableSharedFlow<String>(extraBufferCapacity = 8)
        events.tryEmit("too-early")
        events.test {
            expectNoEvents()
            events.emit("now")
            assertEquals("now", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun extraBufferCapacity_is_not_replay() = runTest {
        val events = MutableSharedFlow<String>(extraBufferCapacity = 8)
        assertTrue(events.tryEmit("buffered-not-replayed"))
        events.test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun replay_delivers_to_a_late_collector() = runTest {
        val events = MutableSharedFlow<String>(replay = 1)
        events.emit("hello")
        events.test {
            assertEquals("hello", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
