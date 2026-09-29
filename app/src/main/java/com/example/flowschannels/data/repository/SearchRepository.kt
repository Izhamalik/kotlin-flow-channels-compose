package com.example.flowschannels.data.repository

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger

data class SearchResult(val title: String, val category: String)

/**
 * Offline stand-in for a search endpoint.
 *
 * [search] is a suspending, *cancellable* call. That is the whole point of the search lesson:
 * when `flatMapLatest` cancels the previous inner flow, the [delay] below is cancelled and the
 * work for the stale query is abandoned instead of racing the newer one.
 */
class SearchRepository(private val latencyMs: Long = 600) {

    private val requests = AtomicInteger(0)
    private val completed = AtomicInteger(0)

    val requestCount: Int get() = requests.get()
    val completedCount: Int get() = completed.get()

    fun resetCounters() {
        requests.set(0)
        completed.set(0)
    }

    suspend fun search(query: String): List<SearchResult> {
        requests.incrementAndGet()
        delay(latencyMs)
        completed.incrementAndGet()
        if (query.isBlank()) return emptyList()
        return Catalog.filter { it.title.contains(query, ignoreCase = true) }
    }

    private companion object {
        val Catalog = listOf(
            SearchResult("Kotlin Flow", "Streams"),
            SearchResult("Kotlin Coroutines", "Concurrency"),
            SearchResult("Kotlin Multiplatform", "Platform"),
            SearchResult("Kotlin Serialization", "Data"),
            SearchResult("Kotlin Symbol Processing", "Build"),
            SearchResult("Kotlinx Datetime", "Data"),
            SearchResult("Channels", "Streams"),
            SearchResult("StateFlow", "Streams"),
            SearchResult("SharedFlow", "Streams"),
            SearchResult("callbackFlow", "Streams"),
            SearchResult("channelFlow", "Streams"),
            SearchResult("Compose Runtime", "UI"),
            SearchResult("Compose Navigation", "UI"),
            SearchResult("Room", "Persistence"),
            SearchResult("Retrofit", "Networking"),
            SearchResult("WorkManager", "Background"),
        )
    }
}
