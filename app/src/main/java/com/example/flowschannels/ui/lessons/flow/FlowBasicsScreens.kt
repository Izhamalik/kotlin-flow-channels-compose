package com.example.flowschannels.ui.lessons.flow

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.flowschannels.core.DemoViewModel
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun FlowBasicsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rememberDemoViewModel()
    val lines = vm.collectLines()
    LessonScreen(
        title = "What is Flow?",
        doc = FlowBasicsDoc,
        onBack = onBack,
        modifier = modifier,
        visuals = {
            PipelineDiagram(
                steps = listOf(
                    "flow { emit(value) }",
                    "operators (map, filter, …)",
                    "collect { use(value) }",
                ),
            )
        },
        demo = {
            ActionGrid(
                listOf(
                    DemoAction("flow { emit }", primary = true) {
                        vm.runExclusive {
                            val numbers = flow {
                                log.emit("building 1")
                                emit(1)
                                delay(300)
                                log.emit("building 2")
                                emit(2)
                                delay(300)
                                log.emit("building 3")
                                emit(3)
                            }
                            numbers.logCollect(log)
                        }
                    },
                    DemoAction("flowOf(1, 2, 3)") {
                        vm.runExclusive {
                            flowOf(1, 2, 3).logCollect(log)
                        }
                    },
                    DemoAction("Stop") { vm.stop() },
                ),
            )
            OutputConsole(lines, onClear = { vm.log.reset() })
            Callout(
                text = "Nothing in flow { } ran until collect started. That is the cold-stream contract.",
                tone = Tone.Highlight,
            )
        },
    )
}

private val FlowBasicsDoc = LessonDoc(
    tagline = "A Flow is a cold, sequential, cancellable stream of values produced on demand.",
    whatIsIt = """
        Flow is Kotlin's type for a stream that can emit zero, one, or many values over time, then
        complete or fail. It is not a data structure you iterate; it is a recipe for producing
        values. The recipe stays idle until someone collects it.
        
        You already know coroutines. Flow sits on top of them: emit is a suspend function, collect
        is a suspend function, and cancellation of the collecting coroutine is what stops the
        stream. This course does not re-teach launch/async. It teaches the stream APIs that
        coroutines made possible.
    """.trimIndent(),
    keyApis = listOf("Flow<T>", "flow { }", "emit", "collect", "flowOf"),
    code = listOf(
        CodeSample(
            title = "A cold stream of three integers",
            code = """
                val numbers: Flow<Int> = flow {
                    emit(1)
                    emit(2)
                    emit(3)
                }
                
                viewModelScope.launch {
                    numbers.collect { value ->
                        println(value)
                    }
                }
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        flow { } is a builder. The lambda is not executed when you assign the variable — only when
        collect runs. Each collect starts a new execution of that lambda. emit suspends until the
        collector has processed the value (unless you insert buffer/conflate later). Completion
        happens when the builder lambda returns; failure happens if it throws. Cancellation
        happens if the coroutine that called collect is cancelled.
    """.trimIndent(),
    useCases = listOf(
        "A stream of database rows from Room",
        "A stream of typed search queries",
        "A stream of location updates (via callbackFlow)",
        "A stream of combined form-field values",
    ),
    whenToUse = listOf(
        "The producer can yield more than one value over time",
        "You want operators (map, debounce, retry) between producer and consumer",
        "You want collection to be cancellable with the UI / ViewModel lifecycle",
    ),
    whenNotToUse = listOf(
        "A single one-shot result — use suspend fun instead of Flow<T>",
        "UI state that must always have a current value — StateFlow",
        "Fire-and-forget events to several listeners — SharedFlow",
        "A dedicated pipe between two coroutines — Channel",
    ),
    mistakes = listOf(
        "Calling a flow builder and expecting work to start without collect",
        "Wrapping every suspend function in flow { emit(api()) } for no reason",
        "Treating Flow as a List you can index into",
        "Collecting on the wrong lifecycle (a started Flow that outlives the UI)",
    ),
)

@Composable
fun ColdFlowScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rememberDemoViewModel()
    val lines = vm.collectLines()
    LessonScreen(
        title = "Cold Flow",
        doc = ColdFlowDoc,
        onBack = onBack,
        modifier = modifier,
        visuals = {
            PipelineDiagram(
                steps = listOf(
                    "val flow = flow { … }   // nothing runs",
                    "collector A.collect()   // run #1",
                    "collector B.collect()   // run #2, independent",
                ),
            )
        },
        demo = {
            ActionGrid(
                listOf(
                    DemoAction("Two collectors, same Flow", primary = true) {
                        vm.runExclusive {
                            val sharedDeclaration = tickingFlow(1..3, delayMs = 250, label = "src", log = log)
                            coroutineScope {
                                launch { sharedDeclaration.logCollect(log, lane = "A") }
                                delay(80)
                                launch { sharedDeclaration.logCollect(log, lane = "B") }
                            }
                        }
                    },
                    DemoAction("Stop") { vm.stop() },
                ),
            )
            OutputConsole(lines, onClear = { vm.log.reset() })
            Callout(
                text = "A and B each get 1, 2, 3. The producer ran twice. Cold means per-collector.",
                tone = Tone.Highlight,
            )
        },
    )
}

private val ColdFlowDoc = LessonDoc(
    tagline = "Cold: each collector starts an independent execution of the producer.",
    whatIsIt = """
        A cold Flow is a factory, not a running process. Declaring the Flow does no I/O, starts
        no timers, and opens no connections. Only collect does. Two collectors mean two
        executions — two Room queries, two network polls, two callback registrations.
        
        This is the default for flow { }, flowOf, callbackFlow, Room's Flow queries, and most
        operator chains. Hot streams (StateFlow, SharedFlow, shareIn/stateIn) are the exception,
        covered later.
    """.trimIndent(),
    keyApis = listOf("flow { }", "collect", "cold vs hot"),
    code = listOf(
        CodeSample(
            title = "One declaration, two independent runs",
            code = """
                val ticks = flow {
                    println("started")
                    emit(1); emit(2)
                }
                
                launch { ticks.collect { println("A ${'$'}it") } }
                launch { ticks.collect { println("B ${'$'}it") } }
                // prints "started" twice
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        Operators on a cold Flow are also cold. map { } does not run until collect. The whole
        chain is a description of work. That is why you can pass a Flow out of a repository
        without the query starting: the ViewModel (or Compose) decides when to collect, and
        therefore when the work exists.
    """.trimIndent(),
    useCases = listOf(
        "Room observe queries — one collector per UI surface, each sees the table",
        "A repository function that should not run until the UI is visible",
        "Retryable network polling that should restart per subscriber",
    ),
    whenToUse = listOf(
        "Work should exist only while someone is listening",
        "Each collector should get the full sequence from the start",
    ),
    whenNotToUse = listOf(
        "You need one shared producer for many UI subscribers — shareIn / stateIn",
        "You need the latest value even before anyone collects — StateFlow",
    ),
    mistakes = listOf(
        "Assuming two collectors share emissions (that is hot, not cold)",
        "Starting expensive work in a Flow constructor rather than inside flow { }",
        "Forgetting that operators do not run until collect",
    ),
)

@Composable
fun FlowLifecycleScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rememberDemoViewModel()
    val lines = vm.collectLines()
    LessonScreen(
        title = "Flow lifecycle",
        doc = LifecycleDoc,
        onBack = onBack,
        modifier = modifier,
        visuals = {
            PipelineDiagram(
                steps = listOf(
                    "Create Flow  (idle)",
                    "Collect",
                    "Emit → transform → collect",
                    "Complete  or  Cancel  or  Throw",
                ),
            )
        },
        demo = {
            ActionGrid(
                listOf(
                    DemoAction("Run to completion", primary = true) {
                        vm.runExclusive {
                            tickingFlow(1..3, 280, "src", log).logCollect(log)
                        }
                    },
                    DemoAction("Cancel mid-stream") {
                        vm.runExclusive {
                            val job = launch { tickingFlow(1..8, 280, "src", log).logCollect(log) }
                            delay(900)
                            job.cancel()
                            log.cancel("cancelled after ~900ms")
                        }
                    },
                    DemoAction("Stop") { vm.stop() },
                ),
            )
            OutputConsole(lines, onClear = { vm.log.reset() })
        },
    )
}

private val LifecycleDoc = LessonDoc(
    tagline = "A Flow starts when collected, then emits, then completes, fails, or is cancelled.",
    whatIsIt = """
        There is no hidden thread keeping a cold Flow alive. Its lifetime is the lifetime of
        collect. That is why collectAsStateWithLifecycle is the Compose default: when the
        Activity is STOPPED, collection (and therefore the producer) stops.
        
        Hot Flows change this: after stateIn/shareIn, the shared producer can outlive a given
        collector. The cold recipe still starts only when the sharing policy says so.
    """.trimIndent(),
    keyApis = listOf("collect", "onStart", "onCompletion", "cancellable"),
    code = listOf(
        CodeSample(
            title = "Hooks around the same collect",
            code = """
                repository.observeUsers()
                    .onStart { emit(emptyList()) }
                    .onCompletion { cause ->
                        // cause == null → completed
                        // cause is CancellationException → cancelled
                        // otherwise → failed
                    }
                    .collect { users -> render(users) }
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        Create: you hold a Flow object. Nothing is subscribed.
        Collect: the upstream lambda runs. Resources open.
        Emit: each value suspends through the operator chain to the collector.
        Complete: the builder returns; collect returns.
        Cancel: the collecting Job is cancelled; emit throws CancellationException; resources
        in finally / awaitClose run.
        Multiple collectors of a cold Flow: each has its own lifecycle.
        Multiple collectors of a hot Flow: they share one upstream lifecycle.
    """.trimIndent(),
    useCases = listOf(
        "Start a Room query when the screen is visible, stop when it is not",
        "Show a loading value via onStart before the first real emission",
        "Close a callback registration in awaitClose / onCompletion",
    ),
    whenToUse = listOf("Always reason about lifecycle before adding shareIn — most Flows should stay cold."),
    whenNotToUse = listOf("Do not keep a collect running in a global scope just to 'stay updated'."),
    mistakes = listOf(
        "Collecting in onCreate without stopping in onDestroy / lifecycle",
        "Thinking onCompletion is not called on cancellation — it is, with a cause",
        "Swallowing CancellationException in a custom operator",
    ),
)
