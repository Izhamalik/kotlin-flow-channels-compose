package com.example.flowschannels.ui.lessons.operators

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.flowschannels.core.logCollect
import com.example.flowschannels.ui.components.ActionGrid
import com.example.flowschannels.ui.components.AdviceCard
import com.example.flowschannels.ui.components.Callout
import com.example.flowschannels.ui.components.CodeSample
import com.example.flowschannels.ui.components.DemoAction
import com.example.flowschannels.ui.components.LessonDoc
import com.example.flowschannels.ui.components.LessonScreen
import com.example.flowschannels.ui.components.LessonSection
import com.example.flowschannels.ui.components.OutputConsole
import com.example.flowschannels.ui.components.Paragraph
import com.example.flowschannels.ui.components.SelectorChips
import com.example.flowschannels.ui.components.Tone
import com.example.flowschannels.ui.components.collectLines
import com.example.flowschannels.ui.components.rememberDemoViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transform

private enum class PlaygroundOp(
    val label: String,
    val what: String,
    val input: String,
    val output: String,
    val useCase: String,
    val behavior: String,
    val mistake: String,
) {
    MAP(
        "map",
        "One output per input, transformed.",
        "1, 2, 3, 4, 5",
        "2, 4, 6, 8, 10",
        "DTO → domain, formatting a label, deriving a boolean from a form.",
        "Preserves timing. Errors in the lambda cancel the stream unless caught upstream.",
        "Doing heavy work in map on the collector's dispatcher — add flowOn above it.",
    ),
    FILTER(
        "filter",
        "Pass values that match; skip the rest. Upstream still emits everything.",
        "1, 2, 3, 4, 5",
        "2, 4",
        "Ignore blank queries, ignore stale statuses, keep only even ticks in a demo.",
        "Skipped values still cost whatever the upstream did to produce them.",
        "Filtering in the UI instead of in the stream, then wondering why extra work ran.",
    ),
    TRANSFORM(
        "transform",
        "Generalised map: 0..N emits per input. You may emit, skip, or fan out.",
        "1, 2, 3",
        "1, 1!, 2, 2!, 3, 3!",
        "Expanding one event into a start + end pair; emitting a cached value then a refresh.",
        "The lambda is suspending. You can delay or collect another Flow with emitAll.",
        "Using map + flatten when transform / transformLatest was the direct API.",
    ),
    ON_EACH(
        "onEach",
        "Side-effect, then forward the same value. Does not start collection by itself.",
        "1, 2, 3",
        "1, 2, 3  (plus a log line per value)",
        "Analytics, debug logging, updating a 'last seen' timestamp.",
        "Runs in the downstream context unless flowOn sits above it.",
        "Putting collect inside onEach, or using onEach as if it were collect.",
    ),
    TAKE(
        "take",
        "Complete after N values and cancel the upstream.",
        "1, 2, 3, 4, 5",
        "1, 2, 3",
        "First page of a feed, 'give me three GPS fixes', tests that must not hang.",
        "Cancellation is cooperative: in-flight delay/IO in the producer is cancelled.",
        "Using take on a StateFlow expecting it to complete — StateFlow never completes.",
    ),
    DROP(
        "drop",
        "Skip the first N values, then pass everything else through.",
        "1, 2, 3, 4, 5",
        "3, 4, 5",
        "Ignore a Room initial empty list, skip a replay you already rendered.",
        "If the stream completes before N values, the collector gets nothing.",
        "Confusing drop with filter { index > N } — drop is positional, not by value.",
    ),
    DISTINCT(
        "distinctUntilChanged",
        "Skip a value if it equals the previous one. Non-consecutive repeats still pass.",
        "1, 1, 2, 2, 3, 1",
        "1, 2, 3, 1",
        "Search queries, theme toggles, any StateFlow you collect as UI.",
        "Equality is Any.equals, or the comparator you pass. Data classes help.",
        "Expecting it to unique the entire history (that would be distinct(), which is different).",
    ),
    DEBOUNCE(
        "debounce",
        "Emit a value only after the upstream stays quiet for the timeout.",
        "1, 2, 3 (fast) then 4 after a pause",
        "3, 4   (1 and 2 were superseded)",
        "Search-as-you-type, window-resize, slider values.",
        "Uses delays; tests must advance virtual time. The last value always has a chance.",
        "Debouncing on the UI thread with a Handler instead of the query Flow.",
    ),
    COLLECT_LATEST(
        "collectLatest",
        "New value cancels the previous collector lambda. Only the latest work finishes.",
        "1, 2, 3, 4, 5 (slow handler)",
        "started 1..5, finished only 5",
        "Applying the latest search result, latest animation, latest config.",
        "Unlike map, this is a terminal. There is also mapLatest for a Flow-returning form.",
        "Using it to collect UI state you must show every emission of — you will skip frames.",
    ),
    BUFFER(
        "buffer",
        "Decouple a fast producer from a slow collector with a queue.",
        "1..5 produced quickly",
        "all 1..5, collector lags behind",
        "Disk reads that burst, then a slow mapper; sensor samples.",
        "Default capacity is 64. Full buffer suspends the producer (backpressure).",
        "Buffering unbounded work into memory and calling it 'performance'.",
    ),
    CONFLATE(
        "conflate",
        "Keep only the latest unread value. Intermediates are dropped.",
        "1..5 produced quickly, slow collector",
        "1, then a later value (intermediates gone)",
        "UI that only cares about the latest progress percent or latest location.",
        "Equivalent in spirit to a CONFLATED channel between producer and collector.",
        "Using conflate on events (snackbars) — dropped events never come back.",
    ),
}

@OptIn(FlowPreview::class)
@Composable
fun OperatorsPlaygroundScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rememberDemoViewModel()
    val lines = vm.collectLines()
    var selected by remember { mutableIntStateOf(0) }
    val op = PlaygroundOp.entries[selected]

    LessonScreen(
        title = "Operators playground",
        doc = PlaygroundDoc,
        onBack = onBack,
        modifier = modifier,
        demo = {
            SelectorChips(
                options = PlaygroundOp.entries.map { it.label },
                selectedIndex = selected,
                onSelect = { selected = it },
                label = "Operator",
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Input", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                Text(op.input, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
                Text("Expected output", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                Text(op.output, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
            }
            ActionGrid(
                listOf(
                    DemoAction("Run ${op.label}", primary = true) {
                        vm.runExclusive { runOperator(op) }
                    },
                    DemoAction("Stop") { vm.stop() },
                ),
            )
            OutputConsole(lines, onClear = { vm.log.reset() })
        },
        extras = {
            LessonSection(title = "What it does") { Paragraph(op.what) }
            LessonSection(title = "Real-world use case") { Paragraph(op.useCase) }
            LessonSection(title = "Important behavior") { Paragraph(op.behavior) }
            AdviceCard("Common mistake", listOf(op.mistake), Tone.Bad)
        },
    )
}

private val PlaygroundDoc = LessonDoc(
    tagline = "Pick an operator. Same input sequence, different output contract.",
    whatIsIt = """
        This is a bench, not a new API. Each operator runs against a small integer stream so the
        console timestamps make the contract obvious: debounce waits, collectLatest cancels,
        buffer lets the producer run ahead, conflate drops the middle.
    """.trimIndent(),
    keyApis = PlaygroundOp.entries.map { it.label },
    code = listOf(
        CodeSample(
            title = "The input used by most operators",
            code = """
                val input = flow {
                    for (n in 1..5) {
                        delay(200)
                        emit(n)
                    }
                }
            """.trimIndent(),
        ),
    ),
    howItWorks = "Select an operator, press Run, compare the live output with the expected output above.",
    useCases = listOf("Learning an operator before using it in production code."),
    whenToUse = listOf("Whenever you are unsure what an operator will drop, delay, or cancel."),
    whenNotToUse = listOf("Do not copy the demo delays into production — they exist to make timing visible."),
    mistakes = listOf("Reading only the expected output and skipping the console — the timestamps are the lesson."),
)

@OptIn(FlowPreview::class)
private suspend fun com.example.flowschannels.core.DemoHandle.runOperator(op: PlaygroundOp) {
    val input = flow {
        val values = if (op == PlaygroundOp.DISTINCT) listOf(1, 1, 2, 2, 3, 1) else (1..5).toList()
        values.forEach { n ->
            delay(if (op == PlaygroundOp.DEBOUNCE) 80 else 180)
            log.emit(n.toString(), "in")
            emit(n)
        }
        if (op == PlaygroundOp.DEBOUNCE) {
            delay(500)
            log.emit("4", "in")
            emit(4)
        }
    }

    when (op) {
        PlaygroundOp.MAP -> input.map { it * 2 }.logCollect(log)
        PlaygroundOp.FILTER -> input.filter { it % 2 == 0 }.logCollect(log)
        PlaygroundOp.TRANSFORM -> input.transform {
            emit(it.toString())
            emit("$it!")
        }.logCollect(log)
        PlaygroundOp.ON_EACH -> input.onEach { log.transform("side $it") }.logCollect(log)
        PlaygroundOp.TAKE -> input.take(3).logCollect(log)
        PlaygroundOp.DROP -> input.drop(2).logCollect(log)
        PlaygroundOp.DISTINCT -> input.distinctUntilChanged().logCollect(log)
        PlaygroundOp.DEBOUNCE -> input.debounce(400).logCollect(log)
        PlaygroundOp.COLLECT_LATEST -> input.collectLatest { value ->
            log.collected("start $value")
            delay(400)
            log.transform("finished $value")
        }
        PlaygroundOp.BUFFER -> input.buffer().onEach { delay(400) }.logCollect(log)
        PlaygroundOp.CONFLATE -> input.conflate().onEach { delay(500) }.logCollect(log)
    }
}
