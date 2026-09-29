package com.example.flowschannels.ui.lessons.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flowschannels.data.repository.SearchRepository
import com.example.flowschannels.data.repository.SearchResult
import com.example.flowschannels.ui.components.Callout
import com.example.flowschannels.ui.components.CodeSample
import com.example.flowschannels.ui.components.LessonDoc
import com.example.flowschannels.ui.components.LessonScreen
import com.example.flowschannels.ui.components.PipelineDiagram
import com.example.flowschannels.ui.components.StatRow
import com.example.flowschannels.ui.components.Tone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Results(val query: String, val items: List<SearchResult>) : SearchUiState
}

class SearchViewModel(
    private val repository: SearchRepository = SearchRepository(latencyMs = 650),
) : ViewModel() {

    private val query = MutableStateFlow("")
    val queryText: StateFlow<String> = query.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val state: StateFlow<SearchUiState> = query
        .debounce(300)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { q ->
            flow {
                if (q.isEmpty()) {
                    emit(SearchUiState.Idle)
                    return@flow
                }
                emit(SearchUiState.Loading)
                emit(SearchUiState.Results(q, repository.search(q)))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState.Idle)

    val requestCount: Int get() = repository.requestCount
    val completedCount: Int get() = repository.completedCount

    fun onQueryChange(value: String) {
        query.update { value }
    }
}

@Composable
fun SearchScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm: SearchViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val query by vm.queryText.collectAsStateWithLifecycle()

    LessonScreen(
        title = "Search example",
        doc = SearchDoc,
        onBack = onBack,
        modifier = modifier,
        visuals = {
            PipelineDiagram(
                steps = listOf(
                    "Compose text field",
                    "query StateFlow",
                    "debounce(300)",
                    "distinctUntilChanged",
                    "flatMapLatest",
                    "SearchRepository.search()",
                    "StateFlow<SearchUiState>",
                    "UI",
                ),
                highlightIndices = setOf(2, 4),
            )
        },
        demo = {
            StatRow(
                listOf(
                    "requests" to vm.requestCount.toString(),
                    "completed" to vm.completedCount.toString(),
                ),
            )
            OutlinedTextField(
                value = query,
                onValueChange = vm::onQueryChange,
                label = { Text("Type K, Ko, Kot, Kotl, Kotlin") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (val s = state) {
                        SearchUiState.Idle -> Text("Type to search the offline catalogue.")
                        SearchUiState.Loading -> CircularProgressIndicator()
                        is SearchUiState.Results -> {
                            Text("Results for \"${s.query}\"", style = MaterialTheme.typography.titleSmall)
                            s.items.forEach { Text("${it.title}  ·  ${it.category}") }
                            if (s.items.isEmpty()) Text("No matches.")
                        }
                    }
                }
            }
            Callout(
                text = "Type quickly. requests will exceed completed: flatMapLatest cancelled the in-flight delay() of stale queries. The repository is fake and offline.",
                tone = Tone.Highlight,
            )
        },
    )
}

private val SearchDoc = LessonDoc(
    tagline = "debounce drops keystrokes, distinctUntilChanged drops duplicates, flatMapLatest drops in-flight searches.",
    whatIsIt = """
        This is the production-shaped pipeline, running against SearchRepository — a
        suspend function with delay, so cancellation is visible as completed < requests.
        
        Type "Kotlin" one letter at a time. Without debounce, every letter hits the
        network. Without flatMapLatest, results for "K" can arrive after "Kotlin" and
        overwrite the UI.
    """.trimIndent(),
    keyApis = listOf("debounce", "distinctUntilChanged", "flatMapLatest", "stateIn"),
    code = listOf(
        CodeSample(
            code = """
                query
                    .debounce(300)
                    .distinctUntilChanged()
                    .flatMapLatest { q ->
                        flow {
                            emit(Loading)
                            emit(Results(repository.search(q)))
                        }
                    }
                    .stateIn(viewModelScope, WhileSubscribed(5_000), Idle)
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        query is a MutableStateFlow updated on every keystroke (immediate UI). The
        search pipeline is slower on purpose. Compare requestCount vs completedCount
        after a burst of typing.
    """.trimIndent(),
    useCases = listOf("Search, city filter, user picker, command palette."),
    whenToUse = listOf("Any typeahead that would otherwise stampede a backend."),
    whenNotToUse = listOf("A Search button that submits once — just a suspend call."),
    mistakes = listOf(
        "flatMapConcat on a search box",
        "Debounce in the TextField with a Handler instead of the query Flow",
        "Not using a cancellable delay/IO in the repository so cancellation does nothing",
    ),
)
