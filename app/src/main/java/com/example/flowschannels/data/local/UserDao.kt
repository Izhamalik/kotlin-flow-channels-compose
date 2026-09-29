package com.example.flowschannels.data.local

import com.example.flowschannels.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Deliberately shaped exactly like a Room DAO.
 *
 * In a Room-backed app this interface would be annotated:
 *
 * ```
 * @Dao
 * interface UserDao {
 *     @Query("SELECT * FROM users ORDER BY name")
 *     fun observeUsers(): Flow<List<UserEntity>>
 *
 *     @Upsert
 *     suspend fun upsert(users: List<UserEntity>)
 * }
 * ```
 *
 * The important contract to internalise is the *return types*:
 * - observation is a **cold [Flow]** that Room re-runs whenever the table is invalidated,
 * - a write is a **suspend function** that completes once, because it is not a stream.
 */
interface UserDao {

    /** Cold stream. A new query is executed per collector, and re-executed on every table write. */
    fun observeUsers(): Flow<List<User>>

    /** Derived stream. In Room this would be `SELECT COUNT(*)`; here it is `map` over the table. */
    fun observeUserCount(): Flow<Int>

    suspend fun upsert(users: List<User>)

    suspend fun insert(user: User)

    suspend fun deleteAll()
}

/**
 * An in-memory DAO with Room's *observation semantics*: any write re-emits to every active
 * collector of [observeUsers].
 *
 * This keeps the app buildable without an annotation processor while behaving identically for the
 * purposes of the lesson. The `Flow + Room` screen shows the real Room declarations side by side.
 */
class InMemoryUserDao(initial: List<User> = emptyList()) : UserDao {

    private val table = MutableStateFlow(initial)

    override fun observeUsers(): Flow<List<User>> = table.asStateFlow()

    override fun observeUserCount(): Flow<Int> = table.map { it.size }.distinctUntilChanged()

    override suspend fun upsert(users: List<User>) {
        table.update { current ->
            val merged = current.associateBy { it.id } + users.associateBy { it.id }
            merged.values.sortedBy { it.name }
        }
    }

    override suspend fun insert(user: User) = upsert(listOf(user))

    override suspend fun deleteAll() {
        table.value = emptyList()
    }
}
