package com.example.flowschannels.ui.lessons.hot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flowschannels.data.local.InMemoryUserDao
import com.example.flowschannels.data.model.User
import com.example.flowschannels.data.remote.FakeUsersApi
import com.example.flowschannels.data.repository.DefaultUserRepository
import com.example.flowschannels.data.repository.UserRepository
import com.example.flowschannels.ui.components.Callout
import com.example.flowschannels.ui.components.CodeSample
import com.example.flowschannels.ui.components.LessonDoc
import com.example.flowschannels.ui.components.LessonScreen
import com.example.flowschannels.ui.components.Tone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UsersUiState {
    data object Loading : UsersUiState
    data class Success(val users: List<User>) : UsersUiState
    data class Error(val message: String) : UsersUiState
}

class UsersUiViewModel(
    private val repository: UserRepository = DefaultUserRepository(
        dao = InMemoryUserDao(),
        api = FakeUsersApi(latencyMs = 900),
    ),
) : ViewModel() {

    private val _state = MutableStateFlow<UsersUiState>(UsersUiState.Loading)
    val state: StateFlow<UsersUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeUsers().collect { users ->
                if (_state.value is UsersUiState.Error) return@collect
                _state.value = UsersUiState.Success(users)
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = UsersUiState.Loading
            try {
                repository.refreshUsers()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = UsersUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

@Composable
fun UiStatePatternScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm: UsersUiViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    LessonScreen(
        title = "UI state pattern",
        doc = UiStateDoc,
        onBack = onBack,
        modifier = modifier,
        demo = {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (val s = state) {
                        is UsersUiState.Loading -> {
                            CircularProgressIndicator()
                            Text("Loading users…", style = MaterialTheme.typography.bodyMedium)
                        }
                        is UsersUiState.Success -> {
                            if (s.users.isEmpty()) {
                                Text("No users yet. Pull to refresh.")
                            } else {
                                s.users.forEach { user ->
                                    Text("${user.name}  ·  ${user.city}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        is UsersUiState.Error -> {
                            Text(s.message, color = MaterialTheme.colorScheme.error)
                            Button(onClick = vm::refresh) { Text("Retry") }
                        }
                    }
                    TextButton(onClick = vm::refresh) { Text("Refresh") }
                }
            }
            Callout(
                text = "The when (state) below is the entire UI. No boolean loading flags, no nullable error string fighting a list.",
                tone = Tone.Highlight,
            )
        },
    )
}

private val UiStateDoc = LessonDoc(
    tagline = "One sealed type, one StateFlow: Loading, Success, Error. The UI becomes a when.",
    whatIsIt = """
        Implicit UI state (users: List, loading: Boolean, error: String?) explodes into
        illegal combinations: loading && error, empty list vs not-yet-loaded. A sealed
        interface makes impossible states unrepresentable.
        
        The ViewModel holds StateFlow<UiState>. Compose collects it. Refresh is a
        suspend function on the repository, not a Flow — it is a one-shot command.
        Observation of the table is the Flow.
    """.trimIndent(),
    keyApis = listOf("StateFlow<UiState>", "sealed interface", "collectAsStateWithLifecycle"),
    code = listOf(
        CodeSample(
            code = """
                sealed interface UiState {
                    data object Loading : UiState
                    data class Success(val users: List<User>) : UiState
                    data class Error(val message: String) : UiState
                }
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        Explicit states simplify Compose: each branch renders one thing. You can still
        keep a previous Success visible under a refresh spinner if you model that as
        Success(users, refreshing = true) — the point is you choose the combinations.
    """.trimIndent(),
    useCases = listOf("Every list/detail screen", "Login results", "File pickers with permission errors"),
    whenToUse = listOf("Any screen with async data that can fail."),
    whenNotToUse = listOf("A local-only counter with no loading — a raw Int StateFlow is enough."),
    mistakes = listOf(
        "Parallel loading + error + data fields that can disagree",
        "Putting navigation events inside UiState (they would replay on rotation)",
    ),
)
