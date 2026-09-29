package com.example.flowschannels.ui.lessons.operators

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
import com.example.flowschannels.ui.components.Tone
import com.example.flowschannels.ui.components.collectLines
import com.example.flowschannels.ui.components.rememberDemoViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transform

@OptIn(FlowPreview::class)
@Composable
fun OperatorsOverviewScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rememberDemoViewModel()
    val lines = vm.collectLines()
    LessonScreen(
        title = "Flow operators",
        doc = OperatorsDoc,
        onBack = onBack,
        modifier = modifier,
        demo = {
            ActionGrid(
                listOf(
                    DemoAction("map × 2", primary = true) {
                        vm.runExclusive {
                            tickingFlow(1..5, 200, "src", log)
                                .map { it * 2 }
                                .logCollect(log)
                        }
                    },
                    DemoAction("filter even") {
                        vm.runExclusive {
                            tickingFlow(1..6, 180, "src", log)
                                .filter { it % 2 == 0 }
                                .logCollect(log)
                        }
                    },
                    DemoAction("transform") {
                        vm.runExclusive {
                            tickingFlow(1..3, 220, "src", log)
                                .transform { value ->
                                    emit("$value")
                                    emit("$value!")
                                }
                                .logCollect(log)
                        }
                    },
                    DemoAction("onEach + take(3)") {
                        vm.runExclusive {
                            tickingFlow(1..8, 180, "src", log)
                                .onEach { log.transform("seen $it") }
                                .take(3)
                                .logCollect(log)
                        }
                    },
                    DemoAction("takeWhile < 4") {
                        vm.runExclusive {
                            tickingFlow(1..8, 180, "src", log)
                                .takeWhile { it < 4 }
                                .logCollect(log)
                        }
                    },
                    DemoAction("drop(2)") {
                        vm.runExclusive {
                            tickingFlow(1..5, 180, "src", log)
                                .drop(2)
                                .logCollect(log)
                        }
                    },
                    DemoAction("distinctUntilChanged") {
                        vm.runExclusive {
                            flow {
                                listOf(1, 1, 2, 2, 2, 3, 3).forEach {
                                    delay(180)
                                    log.emit(it.toString())
                                    emit(it)
                                }
                            }.distinctUntilChanged().logCollect(log)
                        }
                    },
                    DemoAction("debounce 400ms") {
                        vm.runExclusive {
                            flow {
                                listOf(1, 2, 3).forEach {
                                    delay(120)
                                    log.emit("burst $it")
                                    emit(it)
                                }
                                delay(500)
                                log.emit("late 4")
                                emit(4)
                            }.debounce(400).logCollect(log)
                        }
                    },
                    DemoAction("sample 500ms") {
                        vm.runExclusive {
                            tickingFlow(1..8, 120, "src", log)
                                .sample(500)
                                .logCollect(log)
                        }
                    },
                    DemoAction("collectLatest") {
                        vm.runExclusive {
                            tickingFlow(1..5, 150, "src", log).collectLatest { value ->
                                log.collected("start $value")
                                delay(400)
                                log.transform("finished $value")
                            }
                            log.lifecycle("collectLatest completed")
                        }
                    },
                    DemoAction("first / single / reduce") {
                        vm.runExclusive {
                            val first = tickingFlow(1..5, 120, "src", log).first()
                            log.collected("first() = $first")
                            val sum = tickingFlow(1..4, 80, "sum", log).reduce { acc, n -> acc + n }
                            log.collected("reduce + = $sum")
                            val folded = tickingFlow(1..3, 80, "fold", log).fold("acc") { acc, n -> "$acc-$n" }
                            log.collected("fold = $folded")
                            val scanned = tickingFlow(1..3, 80, "scan", log).scan(0) { acc, n -> acc + n }
                            scanned.logCollect(log, lane = "scan")
                            try {
                                tickingFlow(1..2, 80, "single", log).single()
                            } catch (e: Exception) {
                                log.error("single() on 2 values: ${e::class.simpleName}")
                            }
                        }
                    },
                    DemoAction("Stop") { vm.stop() },
                ),
            )
            OutputConsole(lines, onClear = { vm.log.reset() })
            Callout(
                text = "take(3) cancels the upstream after three values — the producer of 1..8 does not finish. That is Flow cancellation, not an error.",
                tone = Tone.Highlight,
            )
        },
    )
}

private val OperatorsDoc = LessonDoc(
    tagline = "Operators are cold adapters: they describe a new Flow without running anything until collect.",
    whatIsIt = """
        Almost every Flow API you will memorise is an operator. map, filter, debounce, catch,
        flowOn, stateIn — they all take a Flow and return a Flow (or, for terminals like collect
        / first / reduce, they suspend until the stream produces an answer).
        
        Intermediate operators are lazy. Terminal operators start collection. The playground
        lesson next door lets you stare at input vs output; this screen is the catalogue.
    """.trimIndent(),
    keyApis = listOf(
        "map", "filter", "transform", "onEach", "take", "takeWhile", "drop",
        "distinctUntilChanged", "debounce", "sample", "collectLatest",
        "first", "single", "reduce", "fold", "scan",
    ),
    code = listOf(
        CodeSample(
            title = "A typical UI query pipeline",
            code = """
                queryFlow
                    .debounce(300)
                    .distinctUntilChanged()
                    .filter { it.isNotBlank() }
                    .map { it.trim() }
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        map     — one output per input, transformed.
        filter  — drop values that fail the predicate; upstream still runs.
        transform — map that can emit 0..N times per input (the generalisation).
        onEach  — side effect, then pass the value through. Not a substitute for collect.
        take / takeWhile — complete (and cancel upstream) after N values / when the predicate fails.
        drop — skip the first N values.
        distinctUntilChanged — skip consecutive duplicates. Non-consecutive repeats still pass.
        debounce — emit a value only after a quiet period. Typing K, Ko, Kot → only the last.
        sample — emit the latest value every period, dropping intermediates.
        collectLatest — cancel the previous collector lambda when a new value arrives.
        first / firstOrNull / single — terminal: they collect until they can return.
        reduce / fold — terminal aggregations. scan / runningFold emit each intermediate.
    """.trimIndent(),
    useCases = listOf(
        "debounce + distinctUntilChanged on a search box",
        "map DTO → domain in the repository",
        "take(1) when you only needed the first Room emission",
        "scan to accumulate download progress",
    ),
    whenToUse = listOf("Whenever the transformation belongs in the stream, not in the UI."),
    whenNotToUse = listOf(
        "Do not map in Compose if the ViewModel already exposed the right type",
        "Do not use single() on a StateFlow — it never completes",
        "Do not use collectLatest for UI state you must render every frame of",
    ),
    mistakes = listOf(
        "Thinking distinctUntilChanged filters all historical duplicates (it is consecutive only)",
        "Using debounce without a test — virtual time in runTest is how you prove it",
        "Forgetting that take() cancels upstream, including in-flight IO",
        "Calling collect inside onEach (nested collection, usually a bug)",
        "Using first() on an infinite hot stream without a timeout",
    ),
)
