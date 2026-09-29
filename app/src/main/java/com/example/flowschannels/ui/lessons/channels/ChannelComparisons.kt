package com.example.flowschannels.ui.lessons.channels

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
import com.example.flowschannels.ui.components.Paragraph
import com.example.flowschannels.ui.components.Tone
import com.example.flowschannels.ui.components.collectLines
import com.example.flowschannels.ui.components.rememberDemoViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch

@Composable
fun FlowVsChannelScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rememberDemoViewModel()
    val lines = vm.collectLines()
    LessonScreen(
        title = "Flow vs Channel",
        doc = FlowVsChannelDoc,
        onBack = onBack,
        modifier = modifier,
        extras = {
            ComparisonTable(
                headers = listOf("Question", "Flow", "Channel"),
                rows = listOf(
                    listOf("Mental model", "A stream of values", "A pipe between coroutines"),
                    listOf("Starts when", "collect (cold) / sharing policy (hot)", "send/receive run"),
                    listOf("Many consumers", "Each gets a full independent run (cold)", "Each value goes to one receiver"),
                    listOf("Operators", "Rich (map, retry, debounce…)", "Almost none — convert with receiveAsFlow"),
                    listOf("Completion", "Builder returns or throws", "close()"),
                    listOf("Backpressure", "buffer / conflate / collectLatest", "capacity + suspend send"),
                ),
            )
            Paragraph(
                """
                    This is not a rule that 'you must never use Channel in UI'. It is a
                    semantic distinction. If you find yourself mapping, retrying, and
                    combining, you wanted a Flow. If you find yourself handing work from
                    coroutine A to coroutine B exactly once, you wanted a Channel.
                """.trimIndent(),
            )
        },
        demo = {
            ActionGrid(
                listOf(
                    DemoAction("Cold Flow, 2 collectors") {
                        vm.runExclusive {
                            val f = flow {
                                emit(1); delay(120); emit(2)
                            }
                            launch { f.logCollect(log, "A") }
                            launch { f.logCollect(log, "B") }
                        }
                    },
                    DemoAction("Channel, 2 receivers", primary = true) {
                        vm.runExclusive {
                            val ch = Channel<Int>(Channel.UNLIMITED)
                            launch { ch.send(1); ch.send(2); ch.close() }
                            launch { for (v in ch) log.receive("A $v", "A") }
                            launch { for (v in ch) log.receive("B $v", "B") }
                        }
                    },
                    DemoAction("Stop") { vm.stop() },
                ),
            )
            OutputConsole(lines, onClear = { vm.log.reset() })
            Callout(
                text = "A and B on the Flow both print 1 and 2. On the Channel they split the values — each integer is delivered once.",
                tone = Tone.Highlight,
            )
        },
    )
}

private val FlowVsChannelDoc = LessonDoc(
    tagline = "Flow is a stream. Channel is communication between coroutines.",
    whatIsIt = """
        Keep that sentence around, then immediately weaken it: a Channel can be exposed
        as a Flow (receiveAsFlow), and a Flow can be collected into a Channel (produce /
        produceIn). The types leak into each other because they share coroutines.
        
        Choose based on delivery: does every subscriber need the data (stream / broadcast)
        or does exactly one consumer own each element (pipe)?
    """.trimIndent(),
    keyApis = listOf("Flow", "Channel", "receiveAsFlow"),
    code = listOf(
        CodeSample(
            code = """
                val stream: Flow<Int> = flow { emit(1) }
                val pipe: Channel<Int> = Channel()
            """.trimIndent(),
        ),
    ),
    howItWorks = "Press both buttons and compare lanes A and B.",
    useCases = listOf("Flow: Room, search, UI state pipelines. Channel: actor-style queues, fan-in."),
    whenToUse = listOf("Default to Flow in app code. Reach for Channel when you need point-to-point."),
    whenNotToUse = listOf("Do not expose Channel from a repository — expose Flow (or suspend)."),
    mistakes = listOf("Using Channel as a 'hot Flow' for UI events and then adding a second collector."),
)

@Composable
fun ChannelVsSharedFlowScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rememberDemoViewModel()
    val lines = vm.collectLines()
    LessonScreen(
        title = "Channel vs SharedFlow",
        doc = ChannelVsSharedDoc,
        onBack = onBack,
        modifier = modifier,
        extras = {
            ComparisonTable(
                headers = listOf("Feature", "Channel", "SharedFlow"),
                rows = listOf(
                    listOf("Delivery", "Point-to-point (one receiver)", "Broadcast (every collector)"),
                    listOf("Replay", "None (queued items wait)", "Configurable"),
                    listOf("Buffering", "capacity argument", "extraBufferCapacity + overflow"),
                    listOf("Events", "Good if one UI collector", "Good if several collectors"),
                    listOf("Cancellation", "Closes communication for that side", "Collector leaves; others stay"),
                    listOf("Operators", "Via receiveAsFlow()", "Is already a Flow"),
                ),
            )
        },
        demo = {
            ActionGrid(
                listOf(
                    DemoAction("Channel → 2 collectors") {
                        vm.runExclusive {
                            val ch = Channel<String>(Channel.UNLIMITED)
                            val f = ch.receiveAsFlow()
                            launch { f.logCollect(log, "A") }
                            launch { f.logCollect(log, "B") }
                            delay(80)
                            ch.send("ping")
                            ch.send("pong")
                            ch.close()
                        }
                    },
                    DemoAction("SharedFlow → 2 collectors", primary = true) {
                        vm.runExclusive {
                            val shared = MutableSharedFlow<String>(extraBufferCapacity = 8)
                            launch { shared.logCollect(log, "A") }
                            launch { shared.logCollect(log, "B") }
                            delay(80)
                            shared.emit("ping")
                            shared.emit("pong")
                        }
                    },
                    DemoAction("Stop") { vm.stop() },
                ),
            )
            OutputConsole(lines, onClear = { vm.log.reset() })
            Callout(
                text = "receiveAsFlow() does not turn a Channel into a broadcast. ping is still consumed once. SharedFlow gives ping to A and B.",
                tone = Tone.Highlight,
            )
        },
    )
}

private val ChannelVsSharedDoc = LessonDoc(
    tagline = "Channel splits work. SharedFlow copies events. receiveAsFlow does not change that.",
    whatIsIt = """
        UI events are the usual debate. If the ViewModel has one collector (the screen),
        Channel.receiveAsFlow() works and you get automatic 'consume once' semantics —
        a second accidental collector will steal events. SharedFlow(replay = 0) will
        deliver to every collector, which is what you want for several LaunchedEffects
        or a screen plus a logger.
        
        Practical default in modern Compose apps: SharedFlow for events, StateFlow for
        state, Channel for internal worker pipelines the UI never sees.
    """.trimIndent(),
    keyApis = listOf("Channel", "SharedFlow", "receiveAsFlow"),
    code = listOf(
        CodeSample(
            code = """
                // broadcast
                private val _events = MutableSharedFlow<UiEvent>()
                
                // point-to-point
                private val work = Channel<Task>(capacity = 16)
            """.trimIndent(),
        ),
    ),
    howItWorks = "The demo is the definition: watch who prints ping.",
    useCases = listOf("SharedFlow: snackbars. Channel: a single decoder coroutine draining camera frames."),
    whenToUse = listOf("SharedFlow when you cannot guarantee a single collector."),
    whenNotToUse = listOf("Channel as an app-wide event bus."),
    mistakes = listOf(
        "receiveAsFlow() + two Compose collectors, then 'events randomly missing'",
        "SharedFlow with no extra buffer, emit from a non-suspend function that you converted to tryEmit dropping events",
    ),
)

@Composable
fun ReceiveAsFlowScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rememberDemoViewModel()
    val lines = vm.collectLines()
    LessonScreen(
        title = "receiveAsFlow",
        doc = ReceiveDoc,
        onBack = onBack,
        modifier = modifier,
        demo = {
            ActionGrid(
                listOf(
                    DemoAction("One collector") {
                        vm.runExclusive {
                            val ch = Channel<Int>(Channel.UNLIMITED)
                            launch {
                                ch.receiveAsFlow().logCollect(log, "only")
                            }
                            delay(50)
                            repeat(3) { ch.send(it + 1) }
                            ch.close()
                        }
                    },
                    DemoAction("Two collectors", primary = true) {
                        vm.runExclusive {
                            val ch = Channel<Int>(Channel.UNLIMITED)
                            val f = ch.receiveAsFlow()
                            launch { f.logCollect(log, "A") }
                            launch { f.logCollect(log, "B") }
                            delay(50)
                            repeat(4) { ch.send(it + 1) }
                            ch.close()
                        }
                    },
                    DemoAction("Stop") { vm.stop() },
                ),
            )
            OutputConsole(lines, onClear = { vm.log.reset() })
        },
    )
}

private val ReceiveDoc = LessonDoc(
    tagline = "receiveAsFlow() is a Flow view of a Channel. It does not copy values to every collector.",
    whatIsIt = """
        After receiveAsFlow you can map, debounce, catch — all the Flow operators.
        Collection still pulls from the same Channel. Two collectors race on receive.
        Values are distributed, not broadcast.
        
        consumeAsFlow() is stricter: the Channel is cancelled when the Flow collector
        cancels, because you declared a single owner.
    """.trimIndent(),
    keyApis = listOf("receiveAsFlow", "consumeAsFlow"),
    code = listOf(
        CodeSample(
            code = """
                val events = channel.receiveAsFlow()
                    .map { toUi(it) }
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        Each collect on receiveAsFlow starts a coroutine that receive()s. There is still
        one queue. Fan-out is load-balancing, not multicast.
    """.trimIndent(),
    useCases = listOf("Exposing an internal Channel to a ViewModel that wants operators."),
    whenToUse = listOf("When you already have a Channel and want a Flow API for one consumer."),
    whenNotToUse = listOf("When you needed multicast — use SharedFlow from the start."),
    mistakes = listOf("Documenting receiveAsFlow as 'Channel to SharedFlow'."),
)
