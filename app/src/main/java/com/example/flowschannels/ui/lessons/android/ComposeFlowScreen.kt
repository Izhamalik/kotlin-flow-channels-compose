package com.example.flowschannels.ui.lessons.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flowschannels.ui.components.ActionGrid
import com.example.flowschannels.ui.components.Callout
import com.example.flowschannels.ui.components.CodeSample
import com.example.flowschannels.ui.components.DemoAction
import com.example.flowschannels.ui.components.LessonDoc
import com.example.flowschannels.ui.components.LessonScreen
import com.example.flowschannels.ui.components.Tone
import com.example.flowschannels.ui.lessons.hot.CounterViewModel
import com.example.flowschannels.ui.lessons.hot.EventsViewModel
import com.example.flowschannels.ui.lessons.hot.UiEvent

@Composable
fun ComposeFlowScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val counter: CounterViewModel = viewModel()
    val eventsVm: EventsViewModel = viewModel()
    val count by counter.count.collectAsStateWithLifecycle()

    LaunchedEffect(eventsVm) {
        eventsVm.events.collect { event ->
            if (event is UiEvent.Navigate) {
                // event collection is not state — no replay on recomposition
            }
        }
    }

    LessonScreen(
        title = "Flow + Compose",
        doc = ComposeDoc,
        onBack = onBack,
        modifier = modifier,
        demo = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text("count = $count", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "This number is StateFlow collected with collectAsStateWithLifecycle. Recomposition is driven by new state, not by collect callbacks you wrote.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            ActionGrid(
                listOf(
                    DemoAction("+1", primary = true, onClick = counter::increment),
                    DemoAction("−1", onClick = counter::decrement),
                    DemoAction("emit nav event", onClick = eventsVm::navigate),
                ),
            )
            Callout(
                text = "State: collectAsStateWithLifecycle. Events: LaunchedEffect + collect (or a SnackbarHost). Do not collect events as state.",
                tone = Tone.Highlight,
            )
        },
    )
}

private val ComposeDoc = LessonDoc(
    tagline = "UI state is StateFlow + collectAsStateWithLifecycle. UI events are SharedFlow + a one-shot collector.",
    whatIsIt = """
        collectAsStateWithLifecycle() is the Compose integration that matches Android
        lifecycles: collection is active at STARTED, cancelled below that. That pauses
        upstream cold work (Room, callbackFlow) when the user cannot see the screen,
        and it cooperates with SharingStarted.WhileSubscribed.
        
        collectAsState() (without lifecycle) keeps collecting in STOPPED — fine for
        some desktop/preview cases, the wrong default on a phone.
        
        Recomposition reads the snapshot State produced by that collection. You do not
        call collect in the composable body.
    """.trimIndent(),
    keyApis = listOf("collectAsStateWithLifecycle", "LaunchedEffect", "StateFlow", "SharedFlow"),
    code = listOf(
        CodeSample(
            title = "State",
            code = """
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                when (uiState) { … }
            """.trimIndent(),
        ),
        CodeSample(
            title = "Events",
            code = """
                LaunchedEffect(viewModel) {
                    viewModel.events.collect { event ->
                        snackbarHost.showSnackbar(event.message)
                    }
                }
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        Lifecycle repeats collection on every STARTED. For StateFlow that re-delivers
        .value (good). For SharedFlow(replay = 0) it does not replay old snackbars
        (good). That pair is the whole UI integration story.
    """.trimIndent(),
    useCases = listOf("Every Compose screen in this app."),
    whenToUse = listOf("Always prefer collectAsStateWithLifecycle for UI state on Android."),
    whenNotToUse = listOf("Do not collect SharedFlow with collectAsState — events become sticky state."),
    mistakes = listOf(
        "collect in the composable body (starts a new collect every recomposition)",
        "LaunchedEffect(Unit) collecting a Flow that should stop in STOPPED, not only DESTROYED",
        "Passing a new Flow instance each recomposition so collection restarts forever",
    ),
)
