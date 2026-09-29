package com.example.flowschannels.ui.lessons.hot

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.flowschannels.ui.components.CodeSample
import com.example.flowschannels.ui.components.ComparisonTable
import com.example.flowschannels.ui.components.LessonDoc
import com.example.flowschannels.ui.components.LessonScreen
import com.example.flowschannels.ui.components.Paragraph

@Composable
fun StateVsSharedScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    LessonScreen(
        title = "StateFlow vs SharedFlow",
        doc = ComparisonDoc,
        onBack = onBack,
        modifier = modifier,
        extras = {
            ComparisonTable(
                headers = listOf("Feature", "StateFlow", "SharedFlow"),
                rows = listOf(
                    listOf("Hot", "Yes", "Yes"),
                    listOf("Current value", "Yes (.value)", "No inherent current value"),
                    listOf("Initial value", "Required", "Not required"),
                    listOf("Replay", "Latest state (always)", "Configurable (0..N)"),
                    listOf("Conflation", "Equal values dropped", "Optional via buffer overflow"),
                    listOf("Completes", "Never", "If you close the source / sharing ends"),
                    listOf("State representation", "Excellent", "Possible, but you reinvent StateFlow"),
                    listOf("Events", "Usually the wrong abstraction", "Common use case (replay = 0)"),
                    listOf("Multiple collectors", "Yes, all see current + updates", "Yes, broadcast"),
                    listOf("Compose", "collectAsStateWithLifecycle", "LaunchedEffect + collect (events)"),
                ),
            )
            Paragraph(
                """
                    Use StateFlow when a new collector must know what is true right now (screen
                    state, logged-in user, selected tab). Use SharedFlow when a new collector
                    must not replay history (a snackbar that already showed, a navigation that
                    already happened).
                    
                    "Use X instead of Y" is incomplete: a SharedFlow(replay = 1) can store state,
                    and a StateFlow can be collected as if it were events — but you then fight
                    the defaults (conflation, initial value, replay). Prefer the type whose
                    defaults match the problem.
                """.trimIndent(),
            )
        },
    )
}

private val ComparisonDoc = LessonDoc(
    tagline = "State answers 'what is true now'. Events answer 'what just happened'. Different types on purpose.",
    whatIsIt = """
        Both are hot. Both support many collectors. The product difference is replay of
        a current value versus configurable broadcast of occurrences.
        
        Putting a snackbar message into StateFlow means rotation shows it again. Putting
        the current user into SharedFlow(replay = 0) means a newly opened screen does not
        know who is logged in until the next login event.
    """.trimIndent(),
    keyApis = listOf("StateFlow", "SharedFlow"),
    code = listOf(
        CodeSample(
            title = "Two fields in one ViewModel, two types",
            code = """
                val uiState: StateFlow<UiState> = …
                val events: SharedFlow<UiEvent> = …
            """.trimIndent(),
        ),
    ),
    howItWorks = "Read the table. The 'why' is in the replay / current-value rows.",
    useCases = listOf("Every non-trivial screen uses both: state for rendering, events for effects."),
    whenToUse = listOf("StateFlow for rendering; SharedFlow (or Channel) for effects."),
    whenNotToUse = listOf("Do not replace both with a single SharedFlow 'for simplicity'."),
    mistakes = listOf(
        "UiState.ShowSnackbar as a state flag that you forget to clear",
        "Collecting events with collectAsStateWithLifecycle — the event becomes state and replays",
    ),
)
