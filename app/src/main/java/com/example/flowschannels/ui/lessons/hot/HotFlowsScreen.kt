package com.example.flowschannels.ui.lessons.hot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flowschannels.core.logCollect
import com.example.flowschannels.core.tickingFlow
import com.example.flowschannels.ui.components.ActionGrid
import com.example.flowschannels.ui.components.Callout
import com.example.flowschannels.ui.components.CodeSample
import com.example.flowschannels.ui.components.ComparisonTable
import com.example.flowschannels.ui.components.DemoAction
import com.example.flowschannels.ui.components.LessonDoc
import com.example.flowschannels.ui.components.LessonScreen
import com.example.flowschannels.ui.components.OutputConsole
import com.example.flowschannels.ui.components.PipelineDiagram
import com.example.flowschannels.ui.components.SideBySide
import com.example.flowschannels.ui.components.StatRow
import com.example.flowschannels.ui.components.Tone
import com.example.flowschannels.ui.components.collectLines
import com.example.flowschannels.ui.components.rememberDemoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun HotFlowsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rememberDemoViewModel()
    val lines = vm.collectLines()
    LessonScreen(
        title = "Hot Flows",
        doc = HotDoc,
        onBack = onBack,
        modifier = modifier,
        visuals = {
            PipelineDiagram(
                steps = listOf(
                    "cold Flow  (recipe)",
                    "shareIn / stateIn / MutableStateFlow",
                    "one running producer",
                    "N collectors share emissions",
                ),
            )
        },
        extras = {
            SideBySide(
                leftTitle = "Cold",
                leftBody = "Each collect starts the producer. Two collectors, two Room queries, two timers.",
                rightTitle = "Hot",
                rightBody = "The producer runs independently. Collectors subscribe to something already (or about to be) running.",
            )
        },
        demo = {
            ActionGrid(
                listOf(
                    DemoAction("Cold: 2 collectors") {
                        vm.runExclusive {
                            val cold = tickingFlow(1..3, 250, "cold", log)
                            launch { cold.logCollect(log, "A") }
                            delay(80)
                            launch { cold.logCollect(log, "B") }
                        }
                    },
                    DemoAction("Hot shareIn: 2 collectors", primary = true) {
                        vm.runExclusive {
                            val hot = tickingFlow(1..4, 280, "hot", log)
                                .shareIn(this, SharingStarted.Eagerly, replay = 0)
                            delay(200)
                            launch { hot.logCollect(log, "A") }
                            delay(400)
                            launch { hot.logCollect(log, "B") }
                            delay(1500)
                        }
                    },
                    DemoAction("Stop") { vm.stop() },
                ),
            )
            OutputConsole(lines, onClear = { vm.log.reset() })
            Callout(
                text = "On shareIn, B does not replay what A already saw (replay = 0). The producer still ran once — look at the 'hot' lane.",
                tone = Tone.Highlight,
            )
        },
    )
}

private val HotDoc = LessonDoc(
    tagline = "A Flow is hot when its producer is not 1:1 with a collector.",
    whatIsIt = """
        Cold is the default. Hot is a decision: you want one upstream (one poll, one
        WebSocket, one expensive combine) and many subscribers.
        
        Three ways to get there:
        • Construct a hot stream directly: MutableStateFlow, MutableSharedFlow.
        • Convert a cold Flow: stateIn, shareIn.
        • Some APIs are already hot (rarely in kotlinx.coroutines itself).
        
        SharingStarted tells stateIn/shareIn when the upstream is allowed to run:
        Eagerly (now), Lazily (first subscriber), WhileSubscribed (while anyone listens,
        with an optional timeout so a config change does not restart work).
    """.trimIndent(),
    keyApis = listOf("StateFlow", "SharedFlow", "stateIn", "shareIn", "SharingStarted"),
    code = listOf(
        CodeSample(
            title = "The ViewModel pattern",
            code = """
                val uiState: StateFlow<UiState> = repository.observeUsers()
                    .map(::toUi)
                    .stateIn(
                        scope = viewModelScope,
                        started = SharingStarted.WhileSubscribed(5_000),
                        initialValue = UiState.Loading,
                    )
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        WhileSubscribed(5000) — stop the upstream 5s after the last collector leaves.
        Rotation does not restart Room. Going to the background eventually does.
        
        Eagerly — upstream starts in the ViewModel init, even with zero collectors.
        Lazily — first collect starts it; it never stops (even at zero subscribers).
    """.trimIndent(),
    useCases = listOf(
        "UI state in a ViewModel (StateFlow)",
        "One WebSocket shared by several screens (SharedFlow / shareIn)",
        "Auth session that must exist even with no UI (Eagerly)",
    ),
    whenToUse = listOf("When starting the producer once per collector would be wrong or expensive."),
    whenNotToUse = listOf(
        "A screen-scoped Room query that should die with the screen — stay cold and collect with lifecycle",
        "Making everything hot 'for consistency'",
    ),
    mistakes = listOf(
        "stateIn with Eagerly for a screen that is rarely opened — work runs in the dark",
        "shareIn replay=0 for state, then a late collector sees nothing",
        "Confusing 'hot' with 'infinite' — a SharedFlow can complete if you close it, StateFlow never completes",
    ),
)
