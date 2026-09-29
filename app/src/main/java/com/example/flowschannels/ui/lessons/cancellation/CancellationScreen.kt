package com.example.flowschannels.ui.lessons.cancellation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.flowschannels.core.logCollect
import com.example.flowschannels.core.tickingFlow
import com.example.flowschannels.ui.components.ActionGrid
import com.example.flowschannels.ui.components.Callout
import com.example.flowschannels.ui.components.CodeSample
import com.example.flowschannels.ui.components.DemoAction
import com.example.flowschannels.ui.components.LessonDoc
import com.example.flowschannels.ui.components.LessonScreen
import com.example.flowschannels.ui.components.OutputConsole
import com.example.flowschannels.ui.components.PipelineDiagram
import com.example.flowschannels.ui.components.Tone
import com.example.flowschannels.ui.components.collectLines
import com.example.flowschannels.ui.components.rememberDemoViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun CancellationScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rememberDemoViewModel()
    val lines = vm.collectLines()
    LessonScreen(
        title = "Flow cancellation",
        doc = CancellationDoc,
        onBack = onBack,
        modifier = modifier,
        visuals = {
            PipelineDiagram(
                steps = listOf(
                    "collect { } runs in a coroutine",
                    "that Job is cancelled",
                    "emit throws CancellationException",
                    "upstream stops  (finally / awaitClose)",
                ),
            )
        },
        demo = {
            ActionGrid(
                listOf(
                    DemoAction("Cancel collect", primary = true) {
                        vm.runExclusive {
                            val job = launch { tickingFlow(1..20, 200, "src", log).logCollect(log) }
                            delay(700)
                            job.cancel()
                            log.cancel("outer job cancelled")
                        }
                    },
                    DemoAction("collectLatest") {
                        vm.runExclusive {
                            tickingFlow(1..5, 150, "src", log).collectLatest { value ->
                                log.collected("work $value")
                                delay(400)
                                log.transform("done $value")
                            }
                        }
                    },
                    DemoAction("flatMapLatest") {
                        vm.runExclusive {
                            tickingFlow(1..4, 160, "q", log)
                                .flatMapLatest { q ->
                                    flow {
                                        log.transform("inner $q start")
                                        delay(500)
                                        emit("result $q")
                                        log.transform("inner $q end")
                                    }
                                }
                                .logCollect(log)
                        }
                    },
                    DemoAction("cooperative check") {
                        vm.runExclusive {
                            flow {
                                for (n in 1..5) {
                                currentCoroutineContext().ensureActive()
                                log.emit("busy $n (isActive=${currentCoroutineContext().isActive})")
                                    // Non-suspending work would ignore cancel without ensureActive.
                                    var x = 0
                                    repeat(300_000) { x += it }
                                    emit(n)
                                }
                            }.logCollect(log)
                        }
                    },
                    DemoAction("Stop") { vm.stop() },
                ),
            )
            OutputConsole(lines, onClear = { vm.log.reset() })
            Callout(
                text = "Leaving this screen cancels viewModelScope — the same mechanism as the Stop button.",
                tone = Tone.Highlight,
            )
        },
    )
}

private val CancellationDoc = LessonDoc(
    tagline = "A Flow stops because its collector's coroutine stops. That is the whole mechanism.",
    whatIsIt = """
        There is no separate "unsubscribe" API on Flow. collect is a suspend function. When
        the Job that called it is cancelled — Stop, ViewModel cleared, lifecycle moved to
        STOPPED, collectLatest switched to a new value, flatMapLatest got a new query —
        emit at the next suspension point throws CancellationException and the producer
        unwinds.
        
        Lifecycle-aware collection in Compose is this idea applied to STARTED: the UI is
        not collecting when the user cannot see it, so Room / sensors / polls pause.
    """.trimIndent(),
    keyApis = listOf("collect", "collectLatest", "flatMapLatest", "viewModelScope", "ensureActive"),
    code = listOf(
        CodeSample(
            title = "Why collectAsStateWithLifecycle exists",
            code = """
                val uiState by viewModel.state.collectAsStateWithLifecycle()
                // collection runs only while the Lifecycle is at least STARTED
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        Cooperative cancellation: delay, emit, withContext, and most I/O in coroutines
        abort. A tight CPU loop does not, unless it calls ensureActive() / isActive.
        
        collectLatest cancels the previous collector lambda, not the whole ViewModel.
        flatMapLatest cancels the previous inner Flow.
        viewModelScope cancels every collect when the ViewModel is cleared.
        Lifecycle collection cancels when the UI leaves STARTED.
    """.trimIndent(),
    useCases = listOf(
        "Stop a Room query when the screen is not visible",
        "Abandon the previous search when the query changes",
        "Cancel a demo when the user presses Stop",
    ),
    whenToUse = listOf("Always collect in a scope whose lifetime matches why you are listening."),
    whenNotToUse = listOf("Do not collect in GlobalScope to 'keep it running' — that fights lifecycle."),
    mistakes = listOf(
        "Swallowing CancellationException in catch (breaks the chain)",
        "Assuming cancel() instantly kills CPU-bound work without ensureActive",
        "Collecting in a LaunchedEffect(Unit) without considering STOPPED vs DESTROYED",
    ),
)
