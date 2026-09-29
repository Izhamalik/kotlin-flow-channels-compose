package com.example.flowschannels.data.repository

import com.example.flowschannels.data.local.UserDao
import com.example.flowschannels.data.model.User
import com.example.flowschannels.data.remote.UsersApi
import com.example.flowschannels.data.remote.toUser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * The boundary that matters most in a Flow codebase.
 *
 * Two different shapes on purpose:
 * - [observeUsers] is a **stream**: the caller wants every future value, so it returns `Flow`.
 * - [refreshUsers] is a **one-shot action**: it has a single outcome, so it is a `suspend fun`.
 *
 * Making `refreshUsers()` return `Flow<Unit>` would force every caller to collect something that
 * emits once, and would hide failures inside a stream that nobody is observing for errors.
 */
interface UserRepository {

    fun observeUsers(): Flow<List<User>>

    fun observeUserCount(): Flow<Int>

    suspend fun refreshUsers()

    suspend fun addRandomUser()
}

class DefaultUserRepository(
    private val dao: UserDao,
    private val api: UsersApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : UserRepository {

    /**
     * `flowOn` is applied at the repository edge so the upstream database work never runs on the
     * main thread, while everything the ViewModel adds downstream stays on the collector's context.
     */
    override fun observeUsers(): Flow<List<User>> = dao.observeUsers().flowOn(ioDispatcher)

    override fun observeUserCount(): Flow<Int> = dao.observeUserCount().flowOn(ioDispatcher)

    override suspend fun refreshUsers() = withContext(ioDispatcher) {
        // The single-shot network result is written into the database; the write is what makes the
        // cold DAO Flow re-emit. The UI never observes this function's return value.
        val users = api.getUsers().map { it.toUser() }
        dao.upsert(users)
    }

    override suspend fun addRandomUser() = withContext(ioDispatcher) {
        val id = (100..999).random()
        dao.insert(
            User(
                id = id,
                name = "New User #$id",
                email = "user$id@flow.dev",
                city = listOf("Oslo", "Lisbon", "Kyoto", "Nairobi").random(),
            ),
        )
    }
}
