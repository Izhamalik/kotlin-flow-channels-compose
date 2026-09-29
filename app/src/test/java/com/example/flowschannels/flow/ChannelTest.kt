package com.example.flowschannels.flow

import app.cash.turbine.test
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelTest {

    @Test
    fun send_then_receive() = runTest {
        val channel = Channel<Int>()
        launch { channel.send(7) }
        assertEquals(7, channel.receive())
        channel.close()
    }

    @Test
    fun trySend_fails_on_full_rendezvous_without_receiver() = runTest {
        val channel = Channel<Int>(Channel.RENDEZVOUS)
        assertTrue(channel.trySend(1).isFailure)
        channel.close()
    }

    @Test
    fun conflated_keeps_latest() = runTest {
        val channel = Channel<Int>(Channel.CONFLATED)
        channel.send(1)
        channel.send(2)
        channel.send(3)
        assertEquals(3, channel.receive())
        channel.close()
    }

    @Test
    fun receiveAsFlow_is_point_to_point() = runTest {
        val channel = Channel<Int>(Channel.UNLIMITED)
        val flow = channel.receiveAsFlow()
        val seen = mutableListOf<Int>()
        val a = launch { flow.collect { seen += it } }
        val b = launch { flow.collect { seen += it } }
        channel.send(1)
        channel.send(2)
        channel.close()
        a.join()
        b.join()
        assertEquals(listOf(1, 2), seen.sorted())
        assertEquals(2, seen.size)
    }

    @Test
    fun receiveAsFlow_turbine() = runTest {
        val channel = Channel<String>(Channel.UNLIMITED)
        launch {
            channel.send("ping")
            channel.close()
        }
        channel.receiveAsFlow().test {
            assertEquals("ping", awaitItem())
            awaitComplete()
        }
    }
}
