package com.example.flowschannels.flow

import app.cash.turbine.test
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FlowOperatorsTest {

    @Test
    fun map_transforms_each_value() = runTest {
        val output = flowOf(1, 2, 3, 4, 5).map { it * 2 }.toList()
        assertEquals(listOf(2, 4, 6, 8, 10), output)
    }

    @Test
    fun filter_keeps_matching_values() = runTest {
        val output = flowOf(1, 2, 3, 4, 5).filter { it % 2 == 0 }.toList()
        assertEquals(listOf(2, 4), output)
    }

    @Test
    fun take_cancels_after_n() = runTest {
        val output = flowOf(1, 2, 3, 4, 5).take(3).toList()
        assertEquals(listOf(1, 2, 3), output)
    }

    @Test
    fun drop_skips_prefix() = runTest {
        val output = flowOf(1, 2, 3, 4, 5).drop(2).toList()
        assertEquals(listOf(3, 4, 5), output)
    }

    @Test
    fun distinctUntilChanged_is_consecutive_only() = runTest {
        val output = flowOf(1, 1, 2, 2, 3, 1).distinctUntilChanged().toList()
        assertEquals(listOf(1, 2, 3, 1), output)
    }

    @Test
    fun scan_emits_each_intermediate_accumulator() = runTest {
        val output = flowOf(1, 2, 3).scan(0) { acc, n -> acc + n }.toList()
        assertEquals(listOf(0, 1, 3, 6), output)
    }

    @Test
    fun turbine_awaits_items_in_order() = runTest {
        flowOf("a", "b").test {
            assertEquals("a", awaitItem())
            assertEquals("b", awaitItem())
            awaitComplete()
        }
    }
}
