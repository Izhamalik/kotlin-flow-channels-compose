package com.example.flowschannels.ui.lessons.channels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.flowschannels.ui.components.SelectorChips
import com.example.flowschannels.ui.components.SideBySide
import com.example.flowschannels.ui.components.StatRow
import com.example.flowschannels.ui.components.Tone
import com.example.flowschannels.ui.components.collectLines
import com.example.flowschannels.ui.components.rememberDemoViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun ChannelsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rememberDemoViewModel()
    val lines = vm.collectLines()
    LessonScreen(
        title = "Channels",
        doc = ChannelsDoc,
        onBack = onBack,
        modifier = modifier,
        extras = {
            SideBySide(
                leftTitle = "Flow",
                leftBody = "A stream of values. Collecting is subscribing to a recipe (cold) or a shared source (hot).",
                rightTitle = "Channel",
                rightBody = "A pipe between coroutines. send and receive are rendezvous with a queue in the middle.",
            )
        },
        demo = {
            ActionGrid(
                listOf(
                    DemoAction("send / receive", primary = true) {
                        vm.runExclusive {
                            val channel = Channel<Int>()
                            launch {
                                for (n in 1..5) {
                                    log.send("send $n")
                                    channel.send(n)
                                }
                                channel.close()
                                log.lifecycle("closed")
                            }
                            launch {
                                for (value in channel) {
                                    log.receive("recv $value")
                                    delay(250)
                                }
                                log.lifecycle("receive loop done")
                            }
                        }
                    },
                    DemoAction("trySend / tryReceive") {
                        vm.runExclusive {
                            val channel = Channel<Int>(capacity = 2)
                            repeat(4) { n ->
                                val sent = channel.trySend(n)
                                log.send("trySend $n → ${if (sent.isSuccess) "ok" else "full"}")
                            }
                            while (true) {
                                val r = channel.tryReceive()
                                if (r.isFailure) break
                                log.receive("tryReceive ${r.getOrNull()}")
                            }
                        }
                    },
                    DemoAction("Stop") { vm.stop() },
                ),
            )
            OutputConsole(lines, onClear = { vm.log.reset() })
        },
    )
}

private val ChannelsDoc = LessonDoc(
    tagline = "A Channel is communication between coroutines: send, receive, close — with a capacity policy.",
    whatIsIt = """
        Flow describes a stream. Channel is a concurrent queue with suspend points.
        The producer calls send (or trySend). The consumer calls receive (or tryReceive,
        or iterates with for (x in channel)). Closing the channel is how you signal
        'no more values'; the receive loop then ends.
        
        Cancellation of either side cancels the communication. A cancelled receive does
        not leave a value half-delivered on a rendezvous channel.
        
        receiveAsFlow() exposes the Channel as a cold-looking Flow that is still
        backed by a single queue — the next lesson covers what that does to extra
        collectors.
    """.trimIndent(),
    keyApis = listOf(
        "Channel<T>", "send", "receive", "trySend", "tryReceive",
        "close", "receiveAsFlow", "consumeEach",
    ),
    code = listOf(
        CodeSample(
            code = """
                private val channel = Channel<Int>()
                
                suspend fun sendValue(value: Int) {
                    channel.send(value)
                }
                
                viewModelScope.launch {
                    for (value in channel) {
                        println(value)
                    }
                }
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        send suspends when the buffer is full (or immediately, on RENDEZVOUS, until a
        receiver is ready). receive suspends when the buffer is empty.
        trySend / tryReceive never suspend: they return a ChannelResult.
        close() is one-shot; further send fails. receive still drains remaining items.
    """.trimIndent(),
    useCases = listOf(
        "A work queue with a single consumer",
        "Fan-in from several producers to one processor",
        "Bridging a callback that must not broadcast (one listener)",
    ),
    whenToUse = listOf("When exactly one coroutine should consume each element (point-to-point)."),
    whenNotToUse = listOf(
        "UI state — StateFlow",
        "Broadcast events to several collectors — SharedFlow",
        "A stream that should restart per collector — cold Flow",
    ),
    mistakes = listOf(
        "Forgetting close() so the for-loop never ends",
        "Sharing one Channel with two UI collectors and wondering who got the snackbar",
        "Using UNLIMITED as a default and leaking memory under a slow consumer",
    ),
)

private enum class CapacityKind(val label: String, val capacity: Int) {
    Rendezvous("RENDEZVOUS", Channel.RENDEZVOUS),
    Buffered("Buffered(2)", 2),
    Unlimited("UNLIMITED", Channel.UNLIMITED),
    Conflated("CONFLATED", Channel.CONFLATED),
}

@Composable
fun ChannelCapacityScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rememberDemoViewModel()
    val lines = vm.collectLines()
    var selected by remember { mutableIntStateOf(0) }
    val kind = CapacityKind.entries[selected]

    LessonScreen(
        title = "Channel capacity",
        doc = CapacityDoc,
        onBack = onBack,
        modifier = modifier,
        demo = {
            SelectorChips(
                options = CapacityKind.entries.map { it.label },
                selectedIndex = selected,
                onSelect = { selected = it },
                label = "Capacity",
            )
            ActionGrid(
                listOf(
                    DemoAction("Fast send, slow receive", primary = true) {
                        vm.runExclusive {
                            val channel = Channel<Int>(kind.capacity)
                            launch {
                                for (n in 1..6) {
                                    log.send("send $n …")
                                    channel.send(n)
                                    log.send("send $n done")
                                }
                                channel.close()
                            }
                            launch {
                                delay(80)
                                for (value in channel) {
                                    log.receive("recv $value (slow)")
                                    delay(500)
                                }
                            }
                        }
                    },
                    DemoAction("Stop") { vm.stop() },
                ),
            )
            OutputConsole(lines, onClear = { vm.log.reset() })
            Callout(
                text = when (kind) {
                    CapacityKind.Rendezvous -> "Send suspends until receive happens. Look at the timestamps: send N done is after recv N."
                    CapacityKind.Buffered -> "Two items fit. The third send waits for a receive to free a slot."
                    CapacityKind.Unlimited -> "Every send returns immediately. Memory grows if the consumer never runs."
                    CapacityKind.Conflated -> "Only the latest unread value is kept. Intermediate numbers disappear."
                },
                tone = Tone.Highlight,
            )
        },
    )
}

private val CapacityDoc = LessonDoc(
    tagline = "Capacity is the backpressure policy of a Channel.",
    whatIsIt = """
        Rendezvous (0) — no buffer. send and receive meet in the middle. Maximum
        synchronisation, zero memory.
        
        Buffered(n) — n slots. send suspends when full. This is classic bounded
        backpressure.
        
        Unlimited — send never suspends. A slow consumer plus a fast producer is an
        OutOfMemoryException in slow motion.
        
        Conflated — buffer of one, overwrite. The consumer sees the newest value and
        skips the rest. Progress bars, latest location, not snackbars.
    """.trimIndent(),
    keyApis = listOf(
        "Channel.RENDEZVOUS", "Channel(capacity)", "Channel.UNLIMITED", "Channel.CONFLATED",
    ),
    code = listOf(
        CodeSample(
            code = """
                Channel<Int>(Channel.RENDEZVOUS)
                Channel<Int>(capacity = 10)
                Channel<Int>(Channel.UNLIMITED)
                Channel<Int>(Channel.CONFLATED)
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        Backpressure = the producer slows down because send suspends.
        Dropping = CONFLATED (or BufferOverflow.DROP_OLDEST on SharedFlow).
        Memory = UNLIMITED, or a buffered channel whose consumer died.
    """.trimIndent(),
    useCases = listOf(
        "Rendezvous: strict hand-off, tests, fan-in with no burst",
        "Buffered: producers that burst, consumers that pace I/O",
        "Conflated: UI that only paints the latest sample",
    ),
    whenToUse = listOf("Pick the smallest buffer that still lets the producer breathe."),
    whenNotToUse = listOf("UNLIMITED as a default. CONFLATED for events that must not be lost."),
    mistakes = listOf(
        "Buffered channel plus two receivers — capacity does not turn it into a broadcast",
        "Measuring 'performance' with UNLIMITED and a dummy consumer",
    ),
)
