package com.example.flowschannels.ui.lessons.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.flowschannels.ui.components.CodeSample
import com.example.flowschannels.ui.components.LessonDoc
import com.example.flowschannels.ui.components.LessonScreen

@Composable
fun TestingScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    LessonScreen(
        title = "Flow testing",
        doc = TestingDoc,
        onBack = onBack,
        modifier = modifier,
    )
}

private val TestingDoc = LessonDoc(
    tagline = "runTest gives you virtual time. Turbine gives you awaitItem / awaitComplete / awaitError. Together they make debounce and cancellation deterministic.",
    whatIsIt = """
        Flow tests live in app/src/test. They are meant to be read:
        
        • FlowOperatorsTest — map, filter, take, scan
        • CombiningFlowsTest — combine vs zip vs merge
        • StateFlowTest / SharedFlowTest
        • ChannelTest — capacity and receiveAsFlow fan-out
        • ErrorAndCancellationTest
        • DebounceAndSearchTest — advanceTimeBy(300) instead of waiting
        • SearchViewModelTest — the real search pipeline
        
        You do not sleep(300) in tests. You advanceTimeBy(300) so debounce fires
        instantly on the test scheduler.
        
        Turbine's test { } collects the Flow and lets you assert the next item.
        cancelAndIgnoreRemainingEvents() is how you stop an infinite StateFlow.
    """.trimIndent(),
    keyApis = listOf("runTest", "advanceTimeBy", "turbine.test", "awaitItem", "awaitComplete", "awaitError"),
    code = listOf(
        CodeSample(
            title = "debounce (virtual time)",
            code = """
                @Test
                fun debounce_emits_after_quiet_period() = runTest {
                    val flow = flow {
                        emit("K")
                        emit("Ko")
                        emit("Kotlin")
                    }.debounce(300)
                
                    flow.test {
                        advanceTimeBy(300)
                        assertEquals("Kotlin", awaitItem())
                        awaitComplete()
                    }
                }
            """.trimIndent(),
        ),
        CodeSample(
            title = "StateFlow",
            code = """
                @Test
                fun stateFlow_replays_current() = runTest {
                    val state = MutableStateFlow(0)
                    state.test {
                        assertEquals(0, awaitItem())
                        state.value = 1
                        assertEquals(1, awaitItem())
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        runTest replaces Dispatchers.Main and delay() with a virtual scheduler.
        Unconfined vs Standard test dispatchers change when a collect sees an emit;
        the tests in this project prefer the defaults and Turbine, which is explicit.
    """.trimIndent(),
    useCases = listOf("Every ViewModel that exposes StateFlow", "Every operator you do not trust yet"),
    whenToUse = listOf("Always test debounce, retry, and cancellation — they are where production bugs hide."),
    whenNotToUse = listOf("Do not use Thread.sleep to 'wait for Flow'."),
    mistakes = listOf(
        "Forgetting awaitComplete() on a finite Flow and hanging the test",
        "Forgetting cancelAndIgnoreRemainingEvents() on StateFlow",
        "Testing Compose instead of the ViewModel for operator behaviour",
    ),
)
