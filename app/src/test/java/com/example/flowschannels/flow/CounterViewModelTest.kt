package com.example.flowschannels.flow

import app.cash.turbine.test
import com.example.flowschannels.MainDispatcherRule
import com.example.flowschannels.ui.lessons.hot.CounterViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CounterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun increment_updates_state_and_derived_even_flag() = runTest {
        val vm = CounterViewModel()
        assertEquals(0, vm.count.value)
        vm.isEven.test {
            // stateIn(WhileSubscribed) only maps while collected — .value would stay at the initial.
            assertEquals(true, awaitItem())
            vm.increment()
            assertEquals(1, vm.count.value)
            assertEquals(false, awaitItem())
            vm.increment()
            assertEquals(2, vm.count.value)
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
