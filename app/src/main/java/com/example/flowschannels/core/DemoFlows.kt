package com.example.flowschannels.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A slow, labelled sequence. Timing is the teaching tool: timestamps in the console are how
 * the learner sees debounce, buffer, conflate, zip vs combine, and Channel rendezvous.
 */
fun tickingFlow(
    values: Iterable<Int> = 1..5,
    delayMs: Long = 350,
    label: String = "src",
    log: EmissionLog? = null,
): Flow<Int> = flow {
    log?.lifecycle("upstream started", label)
    for (value in values) {
        delay(delayMs)
        log?.emit(value.toString(), label)
        emit(value)
    }
    log?.lifecycle("upstream completed", label)
}

fun tickingLetters(
    values: Iterable<String>,
    delayMs: Long = 400,
    label: String = "src",
    log: EmissionLog? = null,
): Flow<String> = flow {
    log?.lifecycle("upstream started", label)
    for (value in values) {
        delay(delayMs)
        log?.emit(value, label)
        emit(value)
    }
    log?.lifecycle("upstream completed", label)
}
