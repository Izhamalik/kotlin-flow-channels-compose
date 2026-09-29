package com.example.flowschannels.ui.lessons.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flowschannels.core.logCollect
import com.example.flowschannels.data.local.InMemoryUserDao
import com.example.flowschannels.data.model.SampleUsers
import com.example.flowschannels.data.remote.FakeUsersApi
import com.example.flowschannels.ui.components.ActionGrid
import com.example.flowschannels.ui.components.Callout
import com.example.flowschannels.ui.components.CodeSample
import com.example.flowschannels.ui.components.DemoAction
import com.example.flowschannels.ui.components.LessonDoc
import com.example.flowschannels.ui.components.LessonScreen
import com.example.flowschannels.ui.components.OutputConsole
import com.example.flowschannels.ui.components.PipelineDiagram
import com.example.flowschannels.ui.components.Tone
import com.example.flowschannels.ui.components.collectLines
import com.example.flowschannels.ui.components.rememberDemoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoomDemoViewModel(
    private val dao: InMemoryUserDao = InMemoryUserDao(SampleUsers.take(2)),
) : ViewModel() {

    val users = dao.observeUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun insert() {
        viewModelScope.launch {
            val id = (10..99).random()
            dao.insert(
                com.example.flowschannels.data.model.User(
                    id = id,
                    name = "User $id",
                    email = "u$id@flow.dev",
                    city = "Remote",
                ),
            )
        }
    }

    fun clear() {
        viewModelScope.launch { dao.deleteAll() }
    }
}

@Composable
fun RoomScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm: RoomDemoViewModel = viewModel()
    val users by vm.users.collectAsStateWithLifecycle()

    LessonScreen(
        title = "Flow + Room",
        doc = RoomDoc,
        onBack = onBack,
        modifier = modifier,
        visuals = {
            PipelineDiagram(
                steps = listOf(
                    "Room @Query Flow<List<User>>",
                    "Repository",
                    "ViewModel StateFlow",
                    "Compose",
                ),
            )
        },
        demo = {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("In-memory DAO with Room's observation contract", style = MaterialTheme.typography.labelMedium)
                    users.forEach { Text("${it.name}  (#${it.id})", style = MaterialTheme.typography.bodyMedium) }
                    if (users.isEmpty()) Text("(empty table)")
                    Button(onClick = vm::insert) { Text("insert — observers re-emit") }
                    Button(onClick = vm::clear) { Text("deleteAll") }
                }
            }
            Callout(
                text = "No polling. The write invalidates the query; the Flow emits a new list. That is why Room + Flow exists.",
                tone = Tone.Highlight,
            )
        },
    )
}

private val RoomDoc = LessonDoc(
    tagline = "Room can return Flow from a @Query. The query re-runs when the table is invalidated.",
    whatIsIt = """
        This app does not ship the Room annotation processor (see the existing Gradle
        rule: do not add dependencies just to decorate a lesson). InMemoryUserDao is
        shaped like the real DAO and preserves the contract that matters:
        
        • observeUsers(): Flow — a stream, re-emits on writes
        • insert/upsert: suspend — a command
        
        Real Room:
        @Query("SELECT * FROM users")
        fun observeUsers(): Flow<List<User>>
        
        Each collector of a Room Flow runs its own query. It is cold. stateIn in the
        ViewModel is how you share one observation with Compose.
    """.trimIndent(),
    keyApis = listOf("Flow<List<T>>", "@Query", "distinctUntilChanged"),
    code = listOf(
        CodeSample(
            title = "What you would write with Room",
            code = """
                @Dao
                interface UserDao {
                    @Query("SELECT * FROM users ORDER BY name")
                    fun observeUsers(): Flow<List<UserEntity>>
                
                    @Upsert
                    suspend fun upsert(users: List<UserEntity>)
                }
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        Room tracks which tables a query reads. On invalidation it re-executes and
        emits. Identical lists can still emit; distinctUntilChanged in the repository
        is a common extra.
    """.trimIndent(),
    useCases = listOf("Any local list that the UI should update without a refresh button."),
    whenToUse = listOf("When the source of truth is the database."),
    whenNotToUse = listOf("A one-off SELECT in a worker — use suspend DAO methods."),
    mistakes = listOf(
        "Collecting the DAO Flow on the main thread without letting Room's dispatcher / flowOn do IO",
        "Calling dao.observeUsers() from the composable on every recomposition without remembering",
    ),
)

@Composable
fun RetrofitScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rememberDemoViewModel()
    val lines = vm.collectLines()
    val api = androidx.compose.runtime.remember { FakeUsersApi(latencyMs = 500) }

    LessonScreen(
        title = "Flow + Retrofit",
        doc = RetrofitDoc,
        onBack = onBack,
        modifier = modifier,
        demo = {
            ActionGrid(
                listOf(
                    DemoAction("suspend getUsers()", primary = true) {
                        vm.runExclusive {
                            log.info("call #${api.callCount + 1}")
                            val users = api.getUsers()
                            users.forEach { log.collected(it.name) }
                        }
                    },
                    DemoAction("poll as Flow") {
                        vm.runExclusive {
                            flow {
                                repeat(3) { i ->
                                    log.emit("poll ${i + 1}")
                                    emit(api.getUsers().size)
                                    delay(400)
                                }
                            }.logCollect(log) { "users=$it" }
                        }
                    },
                    DemoAction("retry-able Flow") {
                        vm.runExclusive {
                            api.failNextCall()
                            flow { emit(api.getUsers().map { it.name }) }
                                .catch { e ->
                                    log.error(e.message ?: "fail")
                                    emit(listOf("fallback"))
                                }
                                .logCollect(log) { it.joinToString() }
                        }
                    },
                    DemoAction("Stop") { vm.stop() },
                ),
            )
            OutputConsole(lines, onClear = { vm.log.reset() })
        },
    )
}

private val RetrofitDoc = LessonDoc(
    tagline = "A single HTTP call is a suspend function. Wrap it in Flow only when you have a stream (poll, combine, retry-as-stream).",
    whatIsIt = """
        UsersApi is a real Retrofit interface. The app never hits the network; FakeUsersApi
        implements the same type so the lesson compiles offline.
        
        Retrofit can generate Flow adapters. That does not mean getUsers(): Flow<List<User>>
        is a better API. One response is one value. suspend fun getUsers() is honest.
        
        Flow around network work is useful for:
        • polling / ticking refresh
        • retryWhen with backoff as a stream
        • combine(localFlow, flow { emit(api()) }) to merge cache + network
        • cancellation of in-flight calls when a flatMapLatest query changes
    """.trimIndent(),
    keyApis = listOf("suspend fun", "flow { emit(api()) }", "retryWhen"),
    code = listOf(
        CodeSample(
            title = "Preferred one-shot",
            code = """
                interface UsersApi {
                    @GET("users")
                    suspend fun getUsers(): List<UserDto>
                }
            """.trimIndent(),
        ),
        CodeSample(
            title = "When a Flow is real",
            code = """
                fun pollUsers(): Flow<List<User>> = flow {
                    while (true) {
                        emit(api.getUsers().map { it.toUser() })
                        delay(30_000)
                    }
                }
            """.trimIndent(),
        ),
    ),
    howItWorks = "The first button is the common case. The second is a stream you opted into.",
    useCases = listOf("Polling dashboards", "Search (see Search lesson)", "Offline-first: Room Flow + suspend refresh"),
    whenToUse = listOf("suspend for one request. Flow for repeating or combining."),
    whenNotToUse = listOf("Every Retrofit method returning Flow — it hides errors in a stream nobody handles."),
    mistakes = listOf(
        "Collecting a one-shot flow { emit(api()) } and never cancelling it because you thought it completed in the adapter",
        "Claiming Retrofit 'is reactive' because of Flow support",
    ),
)
