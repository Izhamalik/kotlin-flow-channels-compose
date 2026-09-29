package com.example.flowschannels.flow

import app.cash.turbine.test
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorAndCancellationTest {

    @Test
    fun catch_recovers_with_a_fallback() = runTest {
        val output = flow {
            emit(1)
            error("boom")
        }.catch { emit(-1) }.toList()
        assertEquals(listOf(1, -1), output)
    }

    @Test
    fun retry_resubscribes_to_cold_upstream() = runTest {
        var attempts = 0
        val output = flow {
            attempts++
            emit(attempts)
            if (attempts < 3) error("not yet")
        }.retry(2).toList()
        assertEquals(listOf(1, 2, 3), output)
        assertEquals(3, attempts)
    }

    @Test
    fun catch_does_not_swallow_cancellation() = runTest {
        var catchRan = false
        val job = launch {
            flow {
                emit(1)
                kotlinx.coroutines.delay(Long.MAX_VALUE)
            }.catch { catchRan = true }.collect { }
        }
        job.cancel()
        job.join()
        assertTrue(!catchRan)
    }

    @Test
    fun turbine_awaitError() = runTest {
        flow<Int> { error("http") }.test {
            val error = awaitError()
            assertEquals("http", error.message)
        }
    }
}
