package com.example.flowschannels.ui.lessons.hot

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flowschannels.ui.components.ActionGrid
import com.example.flowschannels.ui.components.Callout
import com.example.flowschannels.ui.components.CodeSample
import com.example.flowschannels.ui.components.DemoAction
import com.example.flowschannels.ui.components.LessonDoc
import com.example.flowschannels.ui.components.LessonScreen
import com.example.flowschannels.ui.components.SideBySide
import com.example.flowschannels.ui.components.StatRow
import com.example.flowschannels.ui.components.Tone
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface UiEvent {
    data class Snackbar(val message: String) : UiEvent
    data class Toast(val message: String) : UiEvent
    data class Navigate(val route: String) : UiEvent
    data class Dialog(val title: String, val body: String) : UiEvent
}

class EventsViewModel : ViewModel() {

    private val _events = MutableSharedFlow<UiEvent>(
        extraBufferCapacity = 8,
    )
    val events = _events.asSharedFlow()

    val subscriptionCount = _events.subscriptionCount

    fun snackbar() = emit(UiEvent.Snackbar("Saved. This will not replay on rotation."))
    fun toast() = emit(UiEvent.Toast("Toast from SharedFlow"))
    fun navigate() = emit(UiEvent.Navigate("profile/42"))
    fun dialog() = emit(UiEvent.Dialog("Confirm", "SharedFlow delivered a one-shot dialog event."))

    fun tryEmitBurst() {
        repeat(3) { i ->
            val ok = _events.tryEmit(UiEvent.Snackbar("tryEmit #$i"))
            if (!ok) {
                viewModelScope.launch {
                    _events.emit(UiEvent.Snackbar("tryEmit #$i fell back to emit()"))
                }
            }
        }
    }

    private fun emit(event: UiEvent) {
        viewModelScope.launch { _events.emit(event) }
    }
}

@Composable
fun SharedFlowScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm: EventsViewModel = viewModel()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<UiEvent.Dialog?>(null) }
    var lastNav by remember { mutableStateOf("—") }
    val subscribers by vm.subscriptionCount.collectAsStateWithLifecycle()

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is UiEvent.Snackbar -> snackbarHost.showSnackbar(event.message)
                is UiEvent.Toast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is UiEvent.Navigate -> lastNav = event.route
                is UiEvent.Dialog -> dialog = event
            }
        }
    }

    LessonScreen(
        title = "SharedFlow",
        doc = SharedFlowDoc,
        onBack = onBack,
        modifier = modifier,
        extras = {
            SideBySide(
                leftTitle = "State",
                leftBody = "What is true right now. Replay it. Show it after rotation. Conflate equals.",
                rightTitle = "Event",
                rightBody = "Something that happens once. Do not replay it. A late collector should not see a snackbar from ten seconds ago.",
            )
        },
        demo = {
            StatRow(
                listOf(
                    "subscribers" to subscribers.toString(),
                    "last nav" to lastNav,
                    "replay" to "0",
                ),
            )
            ActionGrid(
                listOf(
                    DemoAction("Snackbar", primary = true, onClick = vm::snackbar),
                    DemoAction("Toast", onClick = vm::toast),
                    DemoAction("Navigation", onClick = vm::navigate),
                    DemoAction("Dialog", onClick = vm::dialog),
                    DemoAction("tryEmit burst", onClick = vm::tryEmitBurst),
                ),
            )
            SnackbarHost(snackbarHost)
            Callout(
                text = "Rotate the device after a snackbar. It must not appear again. That is why events are not StateFlow.",
                tone = Tone.Highlight,
            )
        },
    )

    dialog?.let { d ->
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(d.title) },
            text = { Text(d.body) },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("OK") } },
        )
    }
}

private val SharedFlowDoc = LessonDoc(
    tagline = "SharedFlow is a hot, configurable broadcast. With replay = 0 it is the standard UI-event bus inside a ViewModel.",
    whatIsIt = """
        MutableSharedFlow has no required initial value. replay controls how many past
        values a new subscriber is told. extraBufferCapacity plus onBufferOverflow
        control what happens when emitters outrun collectors.
        
        emit is suspending and waits for buffer space (SUSPEND overflow). tryEmit never
        waits; it returns false if it cannot deliver. For UI events, extraBufferCapacity ≥ 1
        with DROP_OLDEST or a small buffer is common so a ViewModel method does not
        have to be suspend.
        
        Multiple collectors each receive the value (broadcast). That is the opposite of
        a Channel, where one receive consumes the element.
    """.trimIndent(),
    keyApis = listOf(
        "SharedFlow", "MutableSharedFlow", "emit", "tryEmit",
        "replay", "extraBufferCapacity", "onBufferOverflow", "asSharedFlow",
    ),
    code = listOf(
        CodeSample(
            code = """
                private val _events = MutableSharedFlow<UiEvent>()
                val events = _events.asSharedFlow()
                
                fun onSaved() {
                    viewModelScope.launch { _events.emit(UiEvent.Snackbar("Saved")) }
                }
            """.trimIndent(),
        ),
    ),
    howItWorks = """
        replay = 0, extraBufferCapacity = 0, overflow SUSPEND: a collector must be
        present or emit suspends. That can deadlock a ViewModel if nobody is collecting
        yet. Give events extraBufferCapacity = 1 (or more) so tryEmit/emit can proceed
        during a configuration change gap, or collect with repeatOnLifecycle so the
        gap is short.
    """.trimIndent(),
    useCases = listOf("Snackbar", "Toast", "Navigation", "One-shot dialogs", "Analytics pings"),
    whenToUse = listOf("When a late subscriber should NOT see old values (replay 0)."),
    whenNotToUse = listOf(
        "Current user, current tab, current query — that is StateFlow",
        "A single consumer pipeline (work queue) — Channel is the better model",
    ),
    mistakes = listOf(
        "replay = 1 for snackbars — they come back after rotation",
        "zero buffer + emit from a non-suspending click handler that you converted to tryEmit, silently dropping",
        "One global SharedFlow for the whole app instead of a ViewModel-scoped one",
    ),
)
