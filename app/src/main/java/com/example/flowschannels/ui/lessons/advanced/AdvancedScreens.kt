package com.example.flowschannels.ui.lessons.advanced

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.flowschannels.core.logCollect
import com.example.flowschannels.data.location.FakeLocationService
import com.example.flowschannels.data.location.locationUpdates
import com.example.flowschannels.ui.components.ActionGrid
import com.example.flowschannels.ui.components.Callout
import com.example.flowschannels.ui.components.CodeSample
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CallbackFlowScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rememberDemoViewModel()
    val lines = vm.collectLines()
    val service = androidx.compose.runtime.remember { FakeLocationService(intervalMs = 700) }
    LessonScreen(
        title = "callbackFlow",
        doc = CallbackDoc,
        onBack = onBack,
        modifier = modifier,
        visuals = {
            PipelineDiagram(
                steps = listOf(
                    "LocationCallback.onLocationChanged",
                    "trySend(location)",
                    "awaitClose { unregister }",
                    "collect in ViewModel / Compose",
                ),
            )
        },
        demo = {
            StatRow(listOf("active listeners" to service.activeListenerCount.toString()))
            ActionGrid(
                listOf(
                    DemoAction("Collect locations", primary = true) {
                        vm.runExclusive {
                            log.info("listeners before = ${service.activeListenerCount}")
                            service.locationUpdates().logCollect(log) {
                                "fix #${it.index}  ${"%.4f".format(it.latitude)}, ${"%.4f".format(it.longitude)}"
                            }
                        }
                    },
                    DemoAction("Fail after 3") {
                        service.failAfter(3)
                        vm.log.info("next collection will fail after 3 fixes")
                    },
                    DemoAction("Stop") {
                        vm.stop()
                        vm.log.lifecycle("listeners after stop = ${service.activeListenerCount}")
                    },
                ),
            )
            OutputConsole(lines, onClear = { vm.log.reset() })
            Callout(
                text = "Press Stop and watch active listeners drop to 0. That is awaitClose. Without it the Timer would leak.",
                tone = Tone.Highlight,
            )
        },
    )
}

private val CallbackDoc = LessonDoc(
    tagline = "callbackFlow adapts a listener API into a cold Flow. awaitClose is the unregister.",
    whatIsIt = """
        GPS, sensors, WebSockets, Firebase, BroadcastReceiver — they all push into a
        callback you register. None of them are Flow. callbackFlow is the bridge:
        
        1. Register in the builder.
        2. trySend from the callback (callbacks are not suspend functions).
        3. awaitClose { unregister }. If you forget this, collection completes
           immediately (the builder returned) and the listener leaks.
        
        Cancellation of collect runs awaitClose. That is how lifecycle-aware collection
        turns into 'stop the GPS'.
        
        close(cause) from onFailure turns a callback error into a Flow exception that
        catch / retry can see.
    """.trimIndent(),
    keyApis = listOf("callbackFlow", "trySend", "awaitClose"),
    code = listOf(
        CodeSample(
            code = """
                fun FakeLocationService.locationUpdates(): Flow<Location> = callbackFlow {
                    val callback = object : LocationCallback {
                        override fun onLocationChanged(location: Location) {
                            trySend(location)
                        }
                        override fun onFailure(error: Throwable) {
                            close(error)
                        }
                    }
                    registerCallback(callback)
                    awaitClose { unregisterCallback(callback) }
                }
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        callbackFlow is implemented with a Channel. trySend uses that channel. The
        default buffer is 64. A slow collector will not block the GPS thread; values
        buffer or drop depending on overflow.
    """.trimIndent(),
    useCases = listOf("Location", "Sensors", "WebSockets", "SDK listeners", "BroadcastReceiver"),
    whenToUse = listOf("Whenever the source is callback-based and you want operators / lifecycle."),
    whenNotToUse = listOf("A suspend API already exists — just call it, or flow { emit(api()) } once."),
    mistakes = listOf(
        "Forgetting awaitClose",
        "Calling send from a callback (it is suspend — you will get the wrong thread / crash)",
        "Registering the listener outside callbackFlow so it is not tied to collection",
    ),
)

@Composable
fun ChannelFlowScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rememberDemoViewModel()
    val lines = vm.collectLines()
    LessonScreen(
        title = "channelFlow",
        doc = ChannelFlowDoc,
        onBack = onBack,
        modifier = modifier,
        extras = {
            SideBySide(
                leftTitle = "flow { }",
                leftBody = "Sequential. emit from the builder coroutine only. Nested launch cannot call emit.",
                rightTitle = "channelFlow { }",
                rightBody = "Concurrent. launch children and send from each. Completes when the builder and children finish.",
            )
        },
        demo = {
            ActionGrid(
                listOf(
                    DemoAction("flow { } sequential") {
                        vm.runExclusive {
                            flow {
                                log.emit("A start")
                                delay(400)
                                emit("A")
                                log.emit("B start")
                                delay(400)
                                emit("B")
                            }.logCollect(log)
                        }
                    },
                    DemoAction("channelFlow parallel", primary = true) {
                        vm.runExclusive {
                            channelFlow {
                                launch {
                                    log.emit("A start")
                                    delay(400)
                                    send("A")
                                }
                                launch {
                                    log.emit("B start")
                                    delay(200)
                                    send("B")
                                }
                            }.logCollect(log)
                        }
                    },
                    DemoAction("Stop") { vm.stop() },
                ),
            )
            OutputConsole(lines, onClear = { vm.log.reset() })
        },
    )
}

private val ChannelFlowDoc = LessonDoc(
    tagline = "channelFlow is a Flow builder that allows concurrent senders.",
    whatIsIt = """
        flow { } is not a coroutineScope you can fan out from. emit is tied to one
        sequential producer. channelFlow { } gives you a ProducerScope: you can launch
        children and send from them. The Flow still looks like a normal cold Flow to
        collectors.
        
        Use it when one collection should start several concurrent pieces of work and
        merge their results — overlapping HTTP calls, a ticker plus a listener, etc.
        
        callbackFlow is a special case of this idea aimed at callbacks (it forces
        awaitClose).
    """.trimIndent(),
    keyApis = listOf("channelFlow", "send", "awaitClose"),
    code = listOf(
        CodeSample(
            code = """
                fun loadProfile(): Flow<Profile> = channelFlow {
                    launch { send(api.user()) }
                    launch { send(api.stats()) }
                }
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        Internally a Channel connects senders to the collector. buffer() on a
        channelFlow is subtle: channelFlow already has a channel. Read the docs before
        stacking buffers.
    """.trimIndent(),
    useCases = listOf("Parallel fetches merged into one stream", "Mixing a callback with a ticker"),
    whenToUse = listOf("When flow { } cannot compile because you need to emit from a child coroutine."),
    whenNotToUse = listOf("Simple sequential emit — flow { } is clearer and slightly cheaper."),
    mistakes = listOf(
        "Forgetting that channelFlow is still cold — two collectors start two parallel fan-outs",
        "Using channelFlow where merge(flowA, flowB) was enough",
    ),
)

@Composable
fun FlowContextScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rememberDemoViewModel()
    val lines = vm.collectLines()
    LessonScreen(
        title = "Flow context",
        doc = ContextDoc,
        onBack = onBack,
        modifier = modifier,
        visuals = {
            PipelineDiagram(
                steps = listOf(
                    "observeUsers()  // upstream",
                    "flowOn(Dispatchers.IO)",
                    "map { toUi(it) }  // downstream, collector context",
                    "collect in viewModelScope (Main)",
                ),
            )
        },
        demo = {
            ActionGrid(
                listOf(
                    DemoAction("flowOn IO", primary = true) {
                        vm.runExclusive {
                            flow {
                                log.emit("upstream ${threadName()}")
                                emit(1)
                                delay(150)
                                emit(2)
                            }
                                .flowOn(Dispatchers.Default)
                                .map {
                                    log.transform("map ${threadName()}")
                                    it
                                }
                                .logCollect(log) { "collect $it on ${threadName()}" }
                        }
                    },
                    DemoAction("Stop") { vm.stop() },
                ),
            )
            OutputConsole(lines, onClear = { vm.log.reset() })
        },
    )
}

private fun threadName(): String = Thread.currentThread().name.substringBefore(" ")

private val ContextDoc = LessonDoc(
    tagline = "flowOn changes the context of everything above it. Below it stays on the collector's context.",
    whatIsIt = """
        This is not a general coroutines lesson. It is the one dispatcher fact Flow
        adds: unlike withContext, which wraps a block, flowOn is an operator with
        upstream/downstream semantics.
        
        Upstream = the producer and operators above flowOn. Downstream = operators
        below flowOn plus collect. A repository should flowOn(IO) at the edge so the
        ViewModel can map on Main without thinking.
        
        withContext inside flow { } also works for a one-off, but flowOn keeps the
        stream shape and applies to the whole upstream.
    """.trimIndent(),
    keyApis = listOf("flowOn"),
    code = listOf(
        CodeSample(
            code = """
                repository.observeUsers()
                    .flowOn(Dispatchers.IO)
                    .map { users -> users.map(::toUi) }
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        Multiple flowOn: the closest one to the producer wins for that segment.
        Collecting on Main does not move Room to Main if flowOn(IO) sits above.
    """.trimIndent(),
    useCases = listOf("Database / disk / CPU above flowOn; UI mapping below."),
    whenToUse = listOf("When the producer must not run on Main."),
    whenNotToUse = listOf("flowOn(Main) 'to be safe' — that is the collector's job, and it hides mistakes."),
    mistakes = listOf(
        "flowOn below map, accidentally running the mapper on IO and then hopping for every item",
        "Thinking collect { } on Main means the whole chain is Main",
    ),
)

@Composable
fun BackpressureScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rememberDemoViewModel()
    val lines = vm.collectLines()
    LessonScreen(
        title = "Backpressure",
        doc = BackpressureDoc,
        onBack = onBack,
        modifier = modifier,
        visuals = {
            PipelineDiagram(
                steps = listOf("Fast producer  1 2 3 4 5 6", "buffer / conflate / collectLatest", "Slow consumer"),
            )
        },
        demo = {
            ActionGrid(
                listOf(
                    DemoAction("None (suspend)") {
                        vm.runExclusive { fastSlow(null).logCollect(log) }
                    },
                    DemoAction("buffer", primary = true) {
                        vm.runExclusive { fastSlow(Mode.Buffer).logCollect(log) }
                    },
                    DemoAction("conflate") {
                        vm.runExclusive { fastSlow(Mode.Conflate).logCollect(log) }
                    },
                    DemoAction("collectLatest") {
                        vm.runExclusive {
                            fastProducer().collectLatest { v ->
                                log.collected("start $v")
                                delay(400)
                                log.transform("done $v")
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

private enum class Mode { Buffer, Conflate }

private fun fastProducer() = flow {
    for (n in 1..6) {
        delay(80)
        emit(n)
    }
}

private fun fastSlow(mode: Mode?) = run {
    val slow = { inner: kotlinx.coroutines.flow.Flow<Int> ->
        inner.map {
            delay(350)
            it
        }
    }
    when (mode) {
        null -> slow(fastProducer())
        Mode.Buffer -> slow(fastProducer().buffer())
        Mode.Conflate -> slow(fastProducer().conflate())
    }
}

private val BackpressureDoc = LessonDoc(
    tagline = "When the producer is faster than the consumer, you must buffer, drop, or skip work.",
    whatIsIt = """
        Default Flow is rendezvous: emit waits until collect finishes the previous
        value. That is backpressure by suspension — safe, and sometimes too slow.
        
        buffer — queue. Producer runs ahead until the queue is full, then suspends.
        conflate — keep only the latest unread. Like Channel.CONFLATED.
        collectLatest — do not queue: cancel the slow work and start the new value.
        
        Channels expose the same ideas as capacity. SharedFlow exposes them as
        extraBufferCapacity and onBufferOverflow (SUSPEND, DROP_OLDEST, DROP_LATEST).
    """.trimIndent(),
    keyApis = listOf("buffer", "conflate", "collectLatest", "Channel capacity", "extraBufferCapacity", "onBufferOverflow"),
    code = listOf(
        CodeSample(
            code = """
                fastTicks
                    .buffer()      // don't lose values, use memory
                    .conflate()    // keep latest
                // or
                fastTicks.collectLatest { render(it) }
            """.trimIndent(),
        ),
    ),
    howItWorks = "Run each button. None: emit and collect timestamps stay coupled. buffer: emits burst first. conflate: numbers skip. collectLatest: only the last 'done' prints.",
    useCases = listOf(
        "buffer: bursty disk reads",
        "conflate: progress 0..100 on a slow UI",
        "collectLatest: latest search rendering",
    ),
    whenToUse = listOf("When you have measured a slow downstream, not as a default on every chain."),
    whenNotToUse = listOf("conflate/DROP on billing events or snackbars."),
    mistakes = listOf(
        "buffer(UNLIMITED) as a performance fix",
        "Assuming buffer preserves timing — it decouples it",
    ),
)
