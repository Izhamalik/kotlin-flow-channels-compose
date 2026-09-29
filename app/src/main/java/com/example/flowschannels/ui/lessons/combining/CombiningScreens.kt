package com.example.flowschannels.ui.lessons.combining

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.flowschannels.core.logCollect
import com.example.flowschannels.ui.components.ActionGrid
import com.example.flowschannels.ui.components.Callout
import com.example.flowschannels.ui.components.CodeSample
import com.example.flowschannels.ui.components.DemoAction
import com.example.flowschannels.ui.components.LessonDoc
import com.example.flowschannels.ui.components.LessonScreen
import com.example.flowschannels.ui.components.MarbleDiagram
import com.example.flowschannels.ui.components.OutputConsole
import com.example.flowschannels.ui.components.SideBySide
import com.example.flowschannels.ui.components.TimelineRow
import com.example.flowschannels.ui.components.Tone
import com.example.flowschannels.ui.components.collectLines
import com.example.flowschannels.ui.components.rememberDemoViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.zip

@Composable
fun CombiningOverviewScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    LessonScreen(
        title = "Combining Flows",
        doc = CombiningDoc,
        onBack = onBack,
        modifier = modifier,
        visuals = {
            MarbleDiagram(
                columns = 6,
                caption = "combine re-emits on either source. zip waits for a partner. merge does not pair.",
                rows = listOf(
                    TimelineRow("name", mapOf(0 to "A", 3 to "B")),
                    TimelineRow("age", mapOf(1 to "1", 2 to "2", 4 to "3")),
                    TimelineRow("comb", mapOf(1 to "A1", 2 to "A2", 3 to "B2", 4 to "B3")),
                ),
            )
        },
        extras = {
            SideBySide(
                leftTitle = "zip",
                leftBody = "Pairs by index. Extra values on the faster side wait. Completes when either side completes.",
                rightTitle = "combine",
                rightBody = "Always the latest of each. First emission waits until every source has at least one value, then any change fires.",
            )
        },
    )
}

private val CombiningDoc = LessonDoc(
    tagline = "Three verbs, three contracts: latest-of-each, pair-by-index, or just interleave.",
    whatIsIt = """
        You often have more than one stream: a name field and an age field, a Room table and a
        network status, a ticker and a filter chip. Kotlin gives you three honest ways to put
        them together. They look similar in the editor and behave nothing like each other.
        
        Open the three dedicated lessons and press the same two buttons — Emit name / Emit age —
        under combine, then zip, then merge.
    """.trimIndent(),
    keyApis = listOf("combine", "zip", "merge"),
    code = listOf(
        CodeSample(
            title = "Read these signatures out loud",
            code = """
                combine(name, age) { n, a -> "${'$'}n - ${'$'}a" }
                name.zip(age) { n, a -> "${'$'}n - ${'$'}a" }
                merge(name, age) // Flow<Any>, no pairing
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        combine — snapshot of latest values. UI state from several sources.
        zip — lock-step pairing. Protocol frames, test fixtures, "nth of A with nth of B".
        merge — union of events. Many notification sources, one collector.
    """.trimIndent(),
    useCases = listOf(
        "combine: form validity from several fields",
        "zip: pairing two paginated APIs that return aligned pages",
        "merge: user events from clicks + deep links + notifications",
    ),
    whenToUse = listOf("When the question is 'what is the current picture?' prefer combine."),
    whenNotToUse = listOf("Do not zip UI state — a late name field would stall the age field forever."),
    mistakes = listOf(
        "Using zip for forms because the lambda looks the same as combine",
        "Expecting merge to give you tuples",
        "Combining infinite hot flows without a sharing strategy and leaking collectors",
    ),
)

@Composable
fun CombineScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    PairingLesson(
        title = "combine",
        doc = CombineDoc,
        onBack = onBack,
        modifier = modifier,
        marble = {
            MarbleDiagram(
                columns = 6,
                caption = "Age ticks twice before name changes: combine emits A1 then A2, then B2.",
                rows = listOf(
                    TimelineRow("name", mapOf(0 to "A", 3 to "B")),
                    TimelineRow("age", mapOf(1 to "1", 2 to "2", 4 to "3")),
                    TimelineRow("out", mapOf(1 to "A1", 2 to "A2", 3 to "B2", 4 to "B3")),
                ),
            )
        },
        run = { names, ages, log ->
            combine(names, ages) { n, a -> "$n - $a" }.logCollect(log)
        },
    )
}

private val CombineDoc = LessonDoc(
    tagline = "combine emits whenever any source emits, using the latest value of every source.",
    whatIsIt = """
        combine is the operator that turns several independent pieces of state into one picture.
        It does not emit until every Flow has produced at least once. After that, any source
        firing produces a new combination immediately — the other sources are not waited on.
    """.trimIndent(),
    keyApis = listOf("combine"),
    code = listOf(
        CodeSample(
            code = """
                val nameFlow: Flow<String>
                val ageFlow: Flow<Int>
                
                combine(nameFlow, ageFlow) { name, age ->
                    "${'$'}name - ${'$'}age"
                }
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        Internal state: one slot per source, initially empty. An emission from source i fills
        slot i. If any slot is still empty, drop the tick. If all slots have a value, invoke
        the transform and emit. Completes when every source has completed (after emitting the
        last combination). Cancelling the collector cancels all sources.
    """.trimIndent(),
    useCases = listOf(
        "Enable the Save button only when name, email and terms are valid",
        "Show users from Room plus a 'is offline' banner from a network Flow",
        "Theme + font scale → a resolved Typography",
    ),
    whenToUse = listOf("The UI needs the latest of each, not a historical pairing."),
    whenNotToUse = listOf(
        "Pairing request/response by index — that is zip",
        "You do not have a value for a source yet and cannot wait — seed it with onStart / stateIn",
    ),
    mistakes = listOf(
        "Wondering why nothing appears — one of the Flows has not emitted yet",
        "Combining a cold Flow that restarts expensive work per collector without sharing it",
    ),
)

@Composable
fun ZipScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    PairingLesson(
        title = "zip",
        doc = ZipDoc,
        onBack = onBack,
        modifier = modifier,
        marble = {
            MarbleDiagram(
                columns = 6,
                caption = "zip pairs 1st with 1st, 2nd with 2nd. A leftover age waits for another name.",
                rows = listOf(
                    TimelineRow("name", mapOf(0 to "A", 3 to "B")),
                    TimelineRow("age", mapOf(1 to "1", 2 to "2", 4 to "3")),
                    TimelineRow("out", mapOf(1 to "A1", 3 to "B2")),
                ),
            )
        },
        run = { names, ages, log ->
            names.zip(ages) { n, a -> "$n - $a" }.logCollect(log)
        },
    )
}

private val ZipDoc = LessonDoc(
    tagline = "zip pairs values by index: the nth of A with the nth of B, regardless of when they arrived.",
    whatIsIt = """
        zip is a lock-step zipper. If ages emits three times and names once, you get one pair
        and two ages sitting in a queue. Combine would have given you three snapshots instead.
        
        Completes as soon as either side completes, discarding leftovers on the other side.
    """.trimIndent(),
    keyApis = listOf("zip"),
    code = listOf(
        CodeSample(
            code = """
                nameFlow.zip(ageFlow) { name, age ->
                    "${'$'}name - ${'$'}age"
                }
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        Each source has a queue. When both queues are non-empty, dequeue one from each, emit
        the pair. Timing of the later of the two arrivals is when the pair comes out.
        This is why zip feels 'fair' and also why it stalls a form.
    """.trimIndent(),
    useCases = listOf(
        "Pairing two files that have the same number of records",
        "Tests where you zip expected Flow with actual Flow",
        "Protocol: sequence numbers that must match",
    ),
    whenToUse = listOf("When index alignment is the meaning of the data."),
    whenNotToUse = listOf("UI state. A user editing only the age field would not update the label."),
    mistakes = listOf(
        "Swapping zip and combine because the lambda is identical",
        "Zipping a finite Flow with StateFlow — zip completes when the finite side completes",
    ),
)

@Composable
fun MergeScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    PairingLesson(
        title = "merge",
        doc = MergeDoc,
        onBack = onBack,
        modifier = modifier,
        marble = {
            MarbleDiagram(
                columns = 6,
                caption = "merge is a union. Values keep their identity; nothing is paired.",
                rows = listOf(
                    TimelineRow("name", mapOf(0 to "A", 3 to "B")),
                    TimelineRow("age", mapOf(1 to "1", 2 to "2", 4 to "3")),
                    TimelineRow("out", mapOf(0 to "A", 1 to "1", 2 to "2", 3 to "B", 4 to "3")),
                ),
            )
        },
        run = { names, ages, log ->
            merge(names, ages).logCollect(log)
        },
    )
}

private val MergeDoc = LessonDoc(
    tagline = "merge interleaves emissions from several Flows into one stream, with no pairing.",
    whatIsIt = """
        merge is the union operator. There is no transform lambda with two arguments, because
        there is no pair. Each value passes through as-is. Types must share a common supertype
        (often a sealed event type).
        
        Both sources are collected concurrently. Completion waits for every source to complete.
    """.trimIndent(),
    keyApis = listOf("merge"),
    code = listOf(
        CodeSample(
            code = """
                val clicks: Flow<UiEvent>
                val deepLinks: Flow<UiEvent>
                val events = merge(clicks, deepLinks)
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        Each source is collected in its own coroutine. Emissions are sent into a shared
        output. There is no buffer of 'the other value' because there is no other value.
    """.trimIndent(),
    useCases = listOf(
        "Several event sources feeding one handler",
        "Merging a local cache stream with a once-off refresh stream of the same type",
        "Combining ticker Flow with a manual 'refresh now' Flow",
    ),
    whenToUse = listOf("When values are homogeneous events, not a snapshot of fields."),
    whenNotToUse = listOf("When you needed the latest name AND age together — that is combine."),
    mistakes = listOf(
        "Merging name and age as Flow<Any> and then parsing strings in the UI",
        "Expecting merge to wait for all sources before the first emission",
    ),
)

@Composable
private fun PairingLesson(
    title: String,
    doc: LessonDoc,
    onBack: () -> Unit,
    marble: @Composable () -> Unit,
    run: suspend (
        names: MutableSharedFlow<String>,
        ages: MutableSharedFlow<Int>,
        log: com.example.flowschannels.core.EmissionLog,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = rememberDemoViewModel()
    val lines = vm.collectLines()
    val names = androidx.compose.runtime.remember {
        MutableSharedFlow<String>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    val ages = androidx.compose.runtime.remember {
        MutableSharedFlow<Int>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    var nameSeq = androidx.compose.runtime.remember { intArrayOf(0) }
    var ageSeq = androidx.compose.runtime.remember { intArrayOf(0) }

    LessonScreen(
        title = title,
        doc = doc,
        onBack = onBack,
        modifier = modifier,
        visuals = { marble() },
        demo = {
            ActionGrid(
                listOf(
                    DemoAction("Start collecting", primary = true) {
                        vm.runExclusive {
                            log.info("waiting for sources…")
                            run(names, ages, log)
                        }
                    },
                    DemoAction("Emit name") {
                        val labels = listOf("Ada", "Grace", "Alan")
                        val next = labels[nameSeq[0] % labels.size]
                        nameSeq[0]++
                        names.tryEmit(next)
                        vm.log.emit("name = $next", "name")
                    },
                    DemoAction("Emit age") {
                        val next = 20 + (ageSeq[0] % 5)
                        ageSeq[0]++
                        ages.tryEmit(next)
                        vm.log.emit("age = $next", "age")
                    },
                    DemoAction("Stop") { vm.stop() },
                ),
            )
            OutputConsole(lines, onClear = { vm.log.reset() })
            Callout(
                text = "Start collecting first. Then tap Emit name / Emit age in any order and watch which combinations come out.",
                tone = Tone.Highlight,
            )
        },
    )
}
