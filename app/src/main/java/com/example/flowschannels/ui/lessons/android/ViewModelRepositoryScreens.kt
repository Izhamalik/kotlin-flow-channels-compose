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
import com.example.flowschannels.data.local.InMemoryUserDao
import com.example.flowschannels.data.model.SampleUsers
import com.example.flowschannels.data.model.User
import com.example.flowschannels.data.remote.FakeUsersApi
import com.example.flowschannels.data.repository.DefaultUserRepository
import com.example.flowschannels.ui.components.Callout
import com.example.flowschannels.ui.components.CodeSample
import com.example.flowschannels.ui.components.LessonDoc
import com.example.flowschannels.ui.components.LessonScreen
import com.example.flowschannels.ui.components.PipelineDiagram
import com.example.flowschannels.ui.components.Tone
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArchitectureViewModel(
    private val repository: DefaultUserRepository = DefaultUserRepository(
        dao = InMemoryUserDao(SampleUsers.take(3)),
        api = FakeUsersApi(latencyMs = 600),
    ),
) : ViewModel() {

    val users: StateFlow<List<User>> = repository.observeUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun refresh() {
        viewModelScope.launch { repository.refreshUsers() }
    }

    fun addUser() {
        viewModelScope.launch { repository.addRandomUser() }
    }
}

@Composable
fun ViewModelFlowScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm: ArchitectureViewModel = viewModel()
    val users by vm.users.collectAsStateWithLifecycle()

    LessonScreen(
        title = "Flow + ViewModel",
        doc = ViewModelDoc,
        onBack = onBack,
        modifier = modifier,
        visuals = {
            PipelineDiagram(
                steps = listOf(
                    "Compose UI",
                    "ViewModel",
                    "StateFlow<List<User>>",
                    "Repository.observeUsers(): Flow",
                    "Data source",
                ),
                highlightIndices = setOf(2),
            )
        },
        demo = {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    users.forEach { Text("${it.name} · ${it.city}", style = MaterialTheme.typography.bodyMedium) }
                    if (users.isEmpty()) Text("Empty — the StateFlow initial value before the first Room-like emission.")
                    Button(onClick = vm::addUser) { Text("addRandomUser()") }
                    Button(onClick = vm::refresh) { Text("refreshUsers()  (suspend)") }
                }
            }
            Callout(
                text = "observeUsers is collected once, in stateIn(viewModelScope). The UI never calls collect itself except through collectAsStateWithLifecycle.",
                tone = Tone.Highlight,
            )
        },
    )
}

private val ViewModelDoc = LessonDoc(
    tagline = "The ViewModel converts cold repository Flows into hot UI StateFlow, and keeps one-shot work as suspend functions.",
    whatIsIt = """
        Compose talks to the ViewModel. The ViewModel talks to the repository. The
        repository exposes:
        • Flow for observation (the table, the session, the connection)
        • suspend for commands (refresh, login, delete)
        
        viewModelScope.launch is used for commands and for stateIn's sharing. It is not
        an invitation to teach launch — it is the scope whose cancellation tears down
        collection when the screen is gone.
    """.trimIndent(),
    keyApis = listOf("viewModelScope", "stateIn", "WhileSubscribed", "collectAsStateWithLifecycle"),
    code = listOf(
        CodeSample(
            code = """
                val users: StateFlow<List<User>> = repository.observeUsers()
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
                
                fun refresh() {
                    viewModelScope.launch { repository.refreshUsers() }
                }
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        WhileSubscribed(5000) lets a configuration change (a few hundred ms without
        collectors) keep the upstream alive. After 5s with no UI, the cold Flow stops.
    """.trimIndent(),
    useCases = listOf("Every feature screen."),
    whenToUse = listOf("Always keep MutableStateFlow private; always collect with lifecycle."),
    whenNotToUse = listOf("Do not launch collect in init and also stateIn the same Flow."),
    mistakes = listOf(
        "UI calling repository.observeUsers().collect in a LaunchedEffect and bypassing the ViewModel",
        "viewModelScope.launch { flow.collect } without catching errors — prefer stateIn + catch",
    ),
)

@Composable
fun RepositoryScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    LessonScreen(
        title = "Flow + Repository",
        doc = RepoDoc,
        onBack = onBack,
        modifier = modifier,
        visuals = {
            PipelineDiagram(
                steps = listOf(
                    "fun observeUsers(): Flow<List<User>>",
                    "suspend fun refreshUsers()",
                ),
            )
        },
    )
}

private val RepoDoc = LessonDoc(
    tagline = "Expose a Flow only when the caller wants a stream of future values. One-shot work stays suspend.",
    whatIsIt = """
        A repository is where the Flow-vs-suspend decision is made. Observe the table:
        Flow. Pull from the network once and write the table: suspend. Combining those
        by wrapping getUsers() in flow { emit(api.getUsers()) } for a single collect is
        usually noise — the ViewModel can just call the suspend function.
        
        Flow is justified when:
        • values will keep changing (Room, snapshots, connection)
        • you need operators (retry a poll, combine local + remote)
        • cancellation should abort in-flight work as the UI leaves
        
        This project uses DefaultUserRepository as the working example. Read that file;
        the comments are the lesson.
    """.trimIndent(),
    keyApis = listOf("Flow", "suspend fun", "flowOn"),
    code = listOf(
        CodeSample(
            code = """
                interface UserRepository {
                    fun observeUsers(): Flow<List<User>>
                    suspend fun refreshUsers()
                }
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        observeUsers() is cold (or a hot StateFlow inside the fake DAO — Room's query
        Flow is cold per collector, backed by invalidation). refreshUsers() writes the
        DAO; the write is what makes observers emit. The UI never collects refresh.
    """.trimIndent(),
    useCases = listOf("Users, session, shopping cart, downloads with progress as a Flow."),
    whenToUse = listOf("Flow for 'keep me updated'. suspend for 'do this now'."),
    whenNotToUse = listOf("fun login(): Flow<User> that emits once — that is a suspend fun with extra steps."),
    mistakes = listOf(
        "Returning Flow from every method because this is a Flow project",
        "Exposing MutableStateFlow from the repository so screens fight over writes",
    ),
)
