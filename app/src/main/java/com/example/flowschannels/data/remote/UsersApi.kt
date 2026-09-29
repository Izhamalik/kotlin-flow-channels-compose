package com.example.flowschannels.data.remote

import com.example.flowschannels.data.model.SampleUsers
import com.example.flowschannels.data.model.User
import kotlinx.coroutines.delay
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * A real Retrofit interface (the annotations are compiled by Retrofit, not by us).
 *
 * Note what it does **not** return: `Flow<List<UserDto>>`. A single HTTP request has exactly one
 * result, so `suspend fun` is the honest signature. The `Flow + Retrofit` lesson explains when
 * wrapping a call in a Flow is genuinely useful (polling, retry-with-backoff, combining with a
 * local cache) and when it is just ceremony.
 */
interface UsersApi {

    @GET("users")
    suspend fun getUsers(): List<UserDto>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Int): UserDto

    @GET("users/search")
    suspend fun searchUsers(@Query("q") query: String): List<UserDto>
}

data class UserDto(
    val id: Int,
    val name: String,
    val email: String,
    val city: String,
)

fun UserDto.toUser(): User = User(id = id, name = name, email = email, city = city)

/**
 * Offline implementation of [UsersApi] so every lesson runs on a plane.
 *
 * It keeps the two properties that make network calls interesting to a Flow learner:
 * latency (so cancellation is observable) and failure (so `catch`/`retry` have something to do).
 */
class FakeUsersApi(
    private val latencyMs: Long = 700,
) : UsersApi {

    private val failNext = AtomicBoolean(false)
    private val calls = AtomicInteger(0)

    val callCount: Int get() = calls.get()

    /** Arms a single failure, so the error-handling and retry demos are reproducible. */
    fun failNextCall() = failNext.set(true)

    override suspend fun getUsers(): List<UserDto> {
        calls.incrementAndGet()
        // delay() is cancellable, which is exactly why a cancelled flatMapLatest inner flow
        // never finishes this "request".
        delay(latencyMs)
        if (failNext.getAndSet(false)) throw IOException("HTTP 503 (simulated)")
        return SampleUsers.map { it.toDto() }
    }

    override suspend fun getUser(id: Int): UserDto {
        calls.incrementAndGet()
        delay(latencyMs / 2)
        return SampleUsers.first { it.id == id }.toDto()
    }

    override suspend fun searchUsers(query: String): List<UserDto> {
        calls.incrementAndGet()
        delay(latencyMs)
        if (failNext.getAndSet(false)) throw IOException("HTTP 503 (simulated)")
        return SampleUsers
            .filter { it.name.contains(query, ignoreCase = true) }
            .map { it.toDto() }
    }
}

private fun User.toDto(): UserDto = UserDto(id = id, name = name, email = email, city = city)
