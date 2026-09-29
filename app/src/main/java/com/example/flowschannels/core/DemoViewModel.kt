package com.example.flowschannels.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Receiver for lesson demos: a [CoroutineScope] plus the shared [EmissionLog].
 *
 * `launch` and `log.emit` are both in scope so examples read like the code in the textbook.
 */
class DemoHandle(
    private val scope: CoroutineScope,
    val log: EmissionLog,
) : CoroutineScope by scope

/**
 * Shared host for every interactive lesson.
 *
 * Collecting a Flow is a coroutine. Cancelling that coroutine (Stop, leaving the screen,
 * `viewModelScope` clearing) is what stops the Flow.
 */
class DemoViewModel : ViewModel() {

    val log = EmissionLog()

    private val jobs = CopyOnWriteArrayList<Job>()

    /**
     * Starts work without touching already-running jobs. Used when a lesson needs a producer
     * *and* a consumer at the same time (Channels).
     */
    fun launch(block: suspend DemoHandle.() -> Unit): Job {
        val job = viewModelScope.launch {
            DemoHandle(this, log).block()
        }
        jobs += job
        job.invokeOnCompletion { jobs.remove(job) }
        return job
    }

    /**
     * The usual "Run example" button: wipe the console, cancel whatever was still collecting,
     * start fresh. Cold Flows restart from the beginning because collection is what starts them.
     */
    fun runExclusive(block: suspend DemoHandle.() -> Unit): Job {
        cancelRunning()
        log.reset()
        return launch(block)
    }

    fun cancelRunning() {
        jobs.toList().forEach { it.cancel() }
        jobs.clear()
    }

    fun stop(reason: String = "collection cancelled") {
        cancelRunning()
        log.cancel(reason)
    }

    override fun onCleared() {
        cancelRunning()
        super.onCleared()
    }
}

/** Collect a Flow into the console, tagging each emission so marble-style logs stay readable. */
suspend fun <T> Flow<T>.logCollect(
    log: EmissionLog,
    lane: String? = null,
    transform: (T) -> String = { it.toString() },
) {
    log.lifecycle("collect started", lane)
    try {
        collect { value -> log.collected(transform(value), lane) }
        log.lifecycle("collect completed", lane)
    } catch (cancelled: CancellationException) {
        log.cancel("collect cancelled", lane)
        throw cancelled
    }
}
