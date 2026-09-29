package com.example.flowschannels.ui.lessons.flatmap

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.flowschannels.core.logCollect
import com.example.flowschannels.ui.components.ActionGrid
import com.example.flowschannels.ui.components.Callout
import com.example.flowschannels.ui.components.CodeSample
import com.example.flowschannels.ui.components.ComparisonTable
import com.example.flowschannels.ui.components.DemoAction
import com.example.flowschannels.ui.components.LessonDoc
import com.example.flowschannels.ui.components.LessonScreen
import com.example.flowschannels.ui.components.OutputConsole
import com.example.flowschannels.ui.components.PipelineDiagram
import com.example.flowschannels.ui.components.Tone
import com.example.flowschannels.ui.components.collectLines
import com.example.flowschannels.ui.components.rememberDemoViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun FlatMapScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rememberDemoViewModel()
    val lines = vm.collectLines()
    LessonScreen(
        title = "flatMap operators",
        doc = FlatMapDoc,
        onBack = onBack,
        modifier = modifier,
        visuals = {
            PipelineDiagram(
                steps = listOf(
                    "query  \"K\" → \"Ko\" → \"Kot\"",
                    "inner Flow: search(\"K\"), search(\"Ko\"), …",
                    "flatMapConcat / Merge / Latest",
                    "results",
                ),
            )
        },
        demo = {
            ActionGrid(
                listOf(
                    DemoAction("flatMapConcat", primary = true) {
                        vm.runExclusive {
                            queries()
                                .onEach { log.emit("query $it", "q") }
                                .flatMapConcat { q -> search(q, log) }
                                .logCollect(log)
                        }
                    },
                    DemoAction("flatMapMerge") {
                        vm.runExclusive {
                            queries()
                                .onEach { log.emit("query $it", "q") }
                                .flatMapMerge { q -> search(q, log) }
                                .logCollect(log)
                        }
                    },
                    DemoAction("flatMapLatest") {
                        vm.runExclusive {
                            queries()
                                .onEach { log.emit("query $it", "q") }
                                .flatMapLatest { q -> search(q, log) }
                                .logCollect(log)
                        }
                    },
                    DemoAction("Stop") { vm.stop() },
                ),
            )
            OutputConsole(lines, onClear = { vm.log.reset() })
            Callout(
                text = "Each inner search takes 500ms. Queries arrive every 180ms. Concat queues them, Merge overlaps them, Latest cancels the stale ones.",
                tone = Tone.Highlight,
            )
        },
        extras = {
            ComparisonTable(
                headers = listOf("Operator", "Inner Flows", "Stale work", "Typical use"),
                rows = listOf(
                    listOf("flatMapConcat", "One at a time, in order", "Runs to completion", "Ordered pagination, serial writes"),
                    listOf("flatMapMerge", "Several at once", "All complete, order not guaranteed", "Fetching many ids concurrently"),
                    listOf("flatMapLatest", "Only the latest", "Previous is cancelled", "Search, typeahead, latest config"),
                ),
            )
        },
    )
}

private fun queries() = flow {
    listOf("K", "Ko", "Kot", "Kotl", "Kotlin").forEach {
        emit(it)
        delay(180)
    }
}

private fun search(query: String, log: com.example.flowschannels.core.EmissionLog) = flow {
    log.transform("search start \"$query\"")
    delay(500)
    log.transform("search done  \"$query\"")
    emit("result($query)")
}

private val FlatMapDoc = LessonDoc(
    tagline = "flatMap* turns each upstream value into an inner Flow, then flattens those inners.",
    whatIsIt = """
        map { fetch(it) } would give you Flow<Flow<Result>>. The flatMap family both maps and
        flattens. The difference between the three is only the policy for in-flight inner Flows
        when the next outer value arrives.
        
        This is the operator family behind every search box. The dedicated Search lesson wires
        it to debounce and a fake repository.
    """.trimIndent(),
    keyApis = listOf("flatMapConcat", "flatMapMerge", "flatMapLatest"),
    code = listOf(
        CodeSample(
            title = "The search shape",
            code = """
                queryFlow
                    .flatMapLatest { query ->
                        repository.search(query) // Flow or flow { emit(suspendSearch(query)) }
                    }
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        flatMapConcat — inner N starts only after inner N-1 completed. Order preserved. Slow.
        flatMapMerge — inners run concurrently (concurrency cap available). Completion order
        is arrival order of inner emissions, not outer order.
        flatMapLatest — a new outer value cancels the current inner. Only the latest query
        is allowed to finish. That is why typing "Kotlin" does not show results for "K".
    """.trimIndent(),
    useCases = listOf(
        "flatMapLatest: search, live filters, 'current user' then their feed",
        "flatMapMerge: download N images, bounded concurrency",
        "flatMapConcat: apply a list of edits in order",
    ),
    whenToUse = listOf("When each upstream value starts a new stream, not just a new mapped value."),
    whenNotToUse = listOf(
        "A suspend one-shot can be map + flow { emit(api()) }, but flatMapLatest is clearer if you need cancellation",
        "Do not flatMapConcat a search box — the user will wait for every stale letter",
    ),
    mistakes = listOf(
        "Using flatMapMerge for search and showing mixed results from K and Kotlin",
        "Forgetting that cancellation of the inner Flow cancels the suspend calls inside it",
        "Confusing mapLatest (values) with collectLatest (terminal lambda)",
    ),
)
