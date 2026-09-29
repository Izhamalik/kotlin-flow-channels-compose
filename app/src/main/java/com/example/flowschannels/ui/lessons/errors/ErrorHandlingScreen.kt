package com.example.flowschannels.ui.lessons.errors

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.flowschannels.core.logCollect
import com.example.flowschannels.ui.components.ActionGrid
import com.example.flowschannels.ui.components.Callout
import com.example.flowschannels.ui.components.CodeSample
import com.example.flowschannels.ui.components.DemoAction
import com.example.flowschannels.ui.components.LessonDoc
import com.example.flowschannels.ui.components.LessonScreen
import com.example.flowschannels.ui.components.OutputConsole
import com.example.flowschannels.ui.components.SideBySide
import com.example.flowschannels.ui.components.Tone
import com.example.flowschannels.ui.components.collectLines
import com.example.flowschannels.ui.components.rememberDemoViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch

@Composable
fun ErrorHandlingScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rememberDemoViewModel()
    val lines = vm.collectLines()
    LessonScreen(
        title = "Error handling",
        doc = ErrorDoc,
        onBack = onBack,
        modifier = modifier,
        extras = {
            SideBySide(
                leftTitle = "Exception",
                leftBody = "Something failed in the producer or an operator. catch / retry can recover. The collector's try/catch around collect also sees it if nobody recovered.",
                rightTitle = "Cancellation",
                rightBody = "The collecting coroutine was cancelled. This is not a failure. catch does not swallow CancellationException. Retrying it would be a bug.",
            )
        },
        demo = {
            ActionGrid(
                listOf(
                    DemoAction("Uncaught fail") {
                        vm.runExclusive {
                            try {
                                boom(at = 2).logCollect(log)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                log.error("collector saw ${e.message}")
                            }
                        }
                    },
                    DemoAction("catch recover", primary = true) {
                        vm.runExclusive {
                            boom(at = 2)
                                .catch { e ->
                                    log.error("caught ${e.message}")
                                    emit(-1)
                                }
                                .logCollect(log)
                        }
                    },
                    DemoAction("retry(2)") {
                        vm.runExclusive {
                            var attempt = 0
                            flow {
                                attempt++
                                log.info("attempt $attempt")
                                emit(1)
                                delay(150)
                                error("boom on attempt $attempt")
                            }
                                .retry(2)
                                .catch { log.error("gave up: ${it.message}") }
                                .logCollect(log)
                        }
                    },
                    DemoAction("retryWhen") {
                        vm.runExclusive {
                            var attempt = 0
                            flow {
                                attempt++
                                log.info("attempt $attempt")
                                emit(1)
                                error("HTTP 503")
                            }
                                .retryWhen { cause, attemptIndex ->
                                    val again = attemptIndex < 2 && cause is IllegalStateException
                                    log.transform("retryWhen attempt=$attemptIndex again=$again")
                                    if (again) delay(200)
                                    again
                                }
                                .catch { log.error("gave up") }
                                .logCollect(log)
                        }
                    },
                    DemoAction("onStart + onCompletion") {
                        vm.runExclusive {
                            boom(at = 9)
                                .onStart { log.lifecycle("onStart"); emit(0) }
                                .onCompletion { cause ->
                                    log.lifecycle("onCompletion cause=${cause?.let { it::class.simpleName } ?: "null"}")
                                }
                                .logCollect(log)
                        }
                    },
                    DemoAction("Cancel is not catch") {
                        vm.runExclusive {
                            val job = launch {
                                ticking()
                                    .catch { log.error("catch saw ${it::class.simpleName}") }
                                    .logCollect(log)
                            }
                            delay(500)
                            job.cancel()
                        }
                    },
                    DemoAction("Stop") { vm.stop() },
                ),
            )
            OutputConsole(lines, onClear = { vm.log.reset() })
            Callout(
                text = "Press “Cancel is not catch”. catch stays silent. onCompletion (if you added it) would still run with a CancellationException cause.",
                tone = Tone.Caution,
            )
        },
    )
}

private fun boom(at: Int) = flow {
    for (n in 1..5) {
        delay(180)
        if (n == at) error("failed at $n")
        emit(n)
    }
}

private fun ticking() = flow {
    var n = 1
    while (true) {
        emit(n++)
        delay(180)
    }
}

private val ErrorDoc = LessonDoc(
    tagline = "catch recovers from upstream exceptions. CancellationException is not an error.",
    whatIsIt = """
        A Flow can complete, fail, or be cancelled. Those are three different endings.
        
        catch sits downstream of the throwing operator and can emit a fallback, complete
        silently, or rethrow. retry / retryWhen resubscribe to the upstream from scratch —
        for a cold Flow that means the producer starts over.
        
        onStart runs when collection begins (you can emit a loading value). onCompletion
        always runs, with cause == null on success, a CancellationException on cancel, or
        the failure on error.
    """.trimIndent(),
    keyApis = listOf("catch", "retry", "retryWhen", "onStart", "onCompletion"),
    code = listOf(
        CodeSample(
            code = """
                repository.observeUsers()
                    .onStart { emit(emptyList()) }
                    .retryWhen { cause, attempt ->
                        cause is IOException && attempt < 3
                    }
                    .catch { emit(emptyList()) }
                    .onCompletion { cause -> log(cause) }
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        Exceptions flow downstream until a catch intercepts them. Operators below catch
        still run. Operators above catch are the ones that failed.
        
        retry(n) is retryWhen { _, attempt -> attempt < n }. Each retry collects the
        upstream again. State inside the flow { } lambda is reset; state you captured
        from outside is not.
        
        CancellationException is used by structured concurrency to stop work. Flow.catch
        does not catch it. If you write a raw try/catch around emit, rethrow it or you
        will break cancellation.
    """.trimIndent(),
    useCases = listOf(
        "Map a network failure to UiState.Error",
        "Retry a flaky poll two times with backoff",
        "Emit a cached list in catch while logging the failure",
    ),
    whenToUse = listOf("When the stream can fail independently of the UI going away."),
    whenNotToUse = listOf(
        "Do not retry CancellationException",
        "Do not catch at the repository and return empty forever without telling the UI",
    ),
    mistakes = listOf(
        "catch { } empty — failures vanish, the UI looks idle",
        "retry() on a hot StateFlow (it does not complete/fail the same way)",
        "try/catch inside flow { } that swallows CancellationException",
        "Putting catch above the operator that actually throws, so it never sees the exception",
    ),
)
