package com.example.flowschannels.ui.lessons.hot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flowschannels.ui.components.ActionGrid
import com.example.flowschannels.ui.components.Callout
import com.example.flowschannels.ui.components.CodeSample
import com.example.flowschannels.ui.components.DemoAction
import com.example.flowschannels.ui.components.LessonDoc
import com.example.flowschannels.ui.components.LessonScreen
import com.example.flowschannels.ui.components.OutputConsole
import com.example.flowschannels.ui.components.StatRow
import com.example.flowschannels.ui.components.Tone
import com.example.flowschannels.ui.components.collectLines
import com.example.flowschannels.ui.components.rememberDemoViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CounterViewModel : ViewModel() {

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    val isEven: StateFlow<Boolean> = count
        .map { it % 2 == 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun increment() {
        _count.update { it + 1 }
    }

    fun decrement() {
        _count.update { it - 1 }
    }

    fun reset() {
        _count.value = 0
    }
}

@Composable
fun StateFlowScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val counter: CounterViewModel = viewModel()
    val count by counter.count.collectAsStateWithLifecycle()
    val even by counter.isEven.collectAsStateWithLifecycle()
    val demo = rememberDemoViewModel()
    val lines = demo.collectLines()

    LessonScreen(
        title = "StateFlow",
        doc = StateFlowDoc,
        onBack = onBack,
        modifier = modifier,
        demo = {
            StatRow(
                listOf(
                    "count" to count.toString(),
                    "isEven" to even.toString(),
                    "value" to counter.count.value.toString(),
                ),
            )
            ActionGrid(
                listOf(
                    DemoAction("increment()", primary = true) {
                        counter.increment()
                        demo.log.collected("count = ${counter.count.value}")
                    },
                    DemoAction("decrement()") {
                        counter.decrement()
                        demo.log.collected("count = ${counter.count.value}")
                    },
                    DemoAction("reset") {
                        counter.reset()
                        demo.log.collected("count = 0")
                    },
                    DemoAction("Log two collectors") {
                        demo.runExclusive {
                            launch { counter.count.onEach { log.collected("A $it", "A") }.collect {} }
                            launch { counter.count.onEach { log.collected("B $it", "B") }.collect {} }
                            repeat(3) {
                                kotlinx.coroutines.delay(300)
                                counter.increment()
                                log.emit("increment → ${counter.count.value}")
                            }
                        }
                    },
                ),
            )
            OutputConsole(lines, onClear = { demo.log.reset() })
            Callout(
                text = "The number above is Compose reading StateFlow via collectAsStateWithLifecycle. Same object as increment() writes.",
                tone = Tone.Highlight,
            )
        },
        extras = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SharingStarted cheat sheet", style = MaterialTheme.typography.titleSmall)
                Text(
                    "WhileSubscribed(5000) — preferred for UI. Eagerly — process-level state. Lazily — first subscriber starts, never stops.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
    )
}

private val StateFlowDoc = LessonDoc(
    tagline = "StateFlow is a hot, conflated, always-has-a-value stream. It is how Compose should see UI state.",
    whatIsIt = """
        StateFlow is a SharedFlow with a required initial value, replay = 1, and no
        completion. Reading .value is O(1) and thread-safe. update { } applies a
        transform atomically — use it when the next state depends on the previous.
        
        asStateFlow() is the encapsulation step: the ViewModel keeps MutableStateFlow
        private so the UI cannot emit. That is not style; it is the boundary.
        
        collectAsStateWithLifecycle() is preferred over collectAsState() because it
        unsubscribes in STOPPED, matching how StateFlow's WhileSubscribed is meant to
        work. Together they pause Room/network work when the user cannot see the screen.
    """.trimIndent(),
    keyApis = listOf(
        "StateFlow", "MutableStateFlow", "value", "update", "asStateFlow",
        "stateIn", "SharingStarted", "WhileSubscribed", "Eagerly", "Lazily",
        "collectAsStateWithLifecycle",
    ),
    code = listOf(
        CodeSample(
            title = "The canonical ViewModel",
            code = """
                class CounterViewModel : ViewModel() {
                    private val _count = MutableStateFlow(0)
                    val count = _count.asStateFlow()
                
                    fun increment() {
                        _count.update { it + 1 }
                    }
                }
            """.trimIndent(),
        ),
        CodeSample(
            title = "Compose",
            code = """
                val count by viewModel.count.collectAsStateWithLifecycle()
                Text("${'$'}count")
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        Every collector is replayed the current value immediately, then sees updates.
        Equal consecutive values (Any.equals) are conflated — setting 3 then 3 does
        not notify. That is why UiState should be a data class / data object.
        
        stateIn converts a cold Flow into a StateFlow in a scope. The initialValue is
        what Compose renders before the first upstream emission.
    """.trimIndent(),
    useCases = listOf(
        "Every screen's UI state",
        "Auth: logged-in user or null",
        "Derived flags via map + stateIn (isEven above)",
    ),
    whenToUse = listOf("The question is 'what is the current UI?' — always StateFlow."),
    whenNotToUse = listOf(
        "Snackbars, toasts, navigation — those are events (SharedFlow / Channel)",
        "A one-shot load with no ongoing observation — a suspend fun is enough",
    ),
    mistakes = listOf(
        "Exposing MutableStateFlow so the UI can write",
        "collect { } in onStart without repeating on STARTED — leaks and double-collects",
        "Using SharedFlow(replay=1) to fake StateFlow and then missing .value",
        "WhileSubscribed(0) causing a restart on every frame of a dialog",
    ),
)
