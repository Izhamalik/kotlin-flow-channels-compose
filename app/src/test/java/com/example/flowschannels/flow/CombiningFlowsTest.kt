package com.example.flowschannels.flow

import app.cash.turbine.test
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CombiningFlowsTest {

    @Test
    fun combine_reemits_on_either_source() = runTest {
        val names = flow {
            emit("Ada")
            delay(20)
            emit("Grace")
        }
        val ages = flow {
            delay(5)
            emit(1)
            delay(10)
            emit(2)
        }
        val output = combine(names, ages) { n, a -> "$n-$a" }.toList()
        assertEquals(listOf("Ada-1", "Ada-2", "Grace-2"), output)
    }

    @Test
    fun zip_pairs_by_index() = runTest {
        val output = flowOf("A", "B").zip(flowOf(1, 2, 3)) { n, a -> "$n$a" }.toList()
        assertEquals(listOf("A1", "B2"), output)
    }

    @Test
    fun merge_interleaves_without_pairing() = runTest {
        merge(flowOf("A"), flowOf("1")).test {
            val items = listOf(awaitItem(), awaitItem())
            assertEquals(setOf("A", "1"), items.toSet())
            awaitComplete()
        }
    }
}
