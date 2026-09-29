package com.example.flowschannels.flow

import app.cash.turbine.test
import com.example.flowschannels.MainDispatcherRule
import com.example.flowschannels.data.repository.SearchRepository
import com.example.flowschannels.ui.lessons.search.SearchUiState
import com.example.flowschannels.ui.lessons.search.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DebounceAndSearchTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun debounce_emits_only_after_quiet_period() = runTest {
        val output = flow {
            emit("K")
            emit("Ko")
            emit("Kotlin")
        }.debounce(300).toList()
        assertEquals(listOf("Kotlin"), output)
    }

    @Test
    fun debounce_emits_intermediate_when_gap_is_long_enough() = runTest {
        val items = mutableListOf<String>()
        flow {
            emit("K")
            kotlinx.coroutines.delay(400)
            emit("Kotlin")
        }.debounce(300).test {
            advanceTimeBy(300)
            runCurrent()
            items += awaitItem()
            advanceTimeBy(400)
            runCurrent()
            items += awaitItem()
            awaitComplete()
        }
        assertEquals(listOf("K", "Kotlin"), items)
    }

    @Test
    fun searchViewModel_idle_until_query() = runTest {
        val vm = SearchViewModel(SearchRepository(latencyMs = 0))
        assertEquals(SearchUiState.Idle, vm.state.value)
    }

    @Test
    fun searchViewModel_debounces_and_returns_results() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = SearchViewModel(SearchRepository(latencyMs = 0))
        vm.state.test {
            assertEquals(SearchUiState.Idle, awaitItem())
            vm.onQueryChange("Kotlin")
            advanceTimeBy(300)
            runCurrent()
            val loading = awaitItem()
            assertEquals(SearchUiState.Loading, loading)
            val results = awaitItem()
            assertTrue(results is SearchUiState.Results)
            assertTrue((results as SearchUiState.Results).items.any { it.title.contains("Kotlin") })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun repository_search_is_cancellable() = runTest {
        val repo = SearchRepository(latencyMs = 1_000)
        val job = launch { repo.search("Kotlin") }
        kotlinx.coroutines.yield()
        job.cancel()
        job.join()
        assertEquals(1, repo.requestCount)
        assertEquals(0, repo.completedCount)
    }
}
