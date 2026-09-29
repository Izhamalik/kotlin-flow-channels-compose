package com.example.flowschannels.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

/**
 * Category of a logged step. The UI colours lines by category so a learner can visually separate
 * "a value was emitted upstream" from "a value arrived at the collector", which is the single most
 * important distinction when reasoning about Flow operators, buffering and backpressure.
 */
enum class LogKind {
    Info,
    Emit,
    Collect,
    Transform,
    Send,
    Receive,
    Lifecycle,
    Error,
    Cancel,
}

data class LogLine(
    val id: Long,
    val elapsedMs: Long,
    val lane: String?,
    val text: String,
    val kind: LogKind,
)

/**
 * A tiny observable console shared by every lesson.
 *
 * It is itself a [MutableStateFlow] holder, so the demos double as a working example of the
 * "private mutable state, public immutable [StateFlow]" pattern taught in the StateFlow lesson.
 *
 * All mutations go through [MutableStateFlow.update], which is atomic. This matters because demos
 * deliberately log from several coroutines at once (producer + consumer).
 */
class EmissionLog(private val maxLines: Int = 250) {

    private val _lines = MutableStateFlow<List<LogLine>>(emptyList())
    val lines: StateFlow<List<LogLine>> = _lines.asStateFlow()

    private val ids = AtomicLong(0L)

    @Volatile
    private var startMs: Long = System.currentTimeMillis()

    /** Restarts the relative clock and empties the console. Call this when a demo re-runs. */
    fun reset() {
        startMs = System.currentTimeMillis()
        _lines.value = emptyList()
    }

    fun add(text: String, kind: LogKind = LogKind.Info, lane: String? = null) {
        val line = LogLine(
            id = ids.incrementAndGet(),
            elapsedMs = System.currentTimeMillis() - startMs,
            lane = lane,
            text = text,
            kind = kind,
        )
        _lines.update { current ->
            val appended = current + line
            if (appended.size > maxLines) appended.takeLast(maxLines) else appended
        }
    }

    fun info(text: String, lane: String? = null) = add(text, LogKind.Info, lane)
    fun emit(text: String, lane: String? = null) = add(text, LogKind.Emit, lane)
    fun collected(text: String, lane: String? = null) = add(text, LogKind.Collect, lane)
    fun transform(text: String, lane: String? = null) = add(text, LogKind.Transform, lane)
    fun send(text: String, lane: String? = null) = add(text, LogKind.Send, lane)
    fun receive(text: String, lane: String? = null) = add(text, LogKind.Receive, lane)
    fun lifecycle(text: String, lane: String? = null) = add(text, LogKind.Lifecycle, lane)
    fun error(text: String, lane: String? = null) = add(text, LogKind.Error, lane)
    fun cancel(text: String, lane: String? = null) = add(text, LogKind.Cancel, lane)
}
