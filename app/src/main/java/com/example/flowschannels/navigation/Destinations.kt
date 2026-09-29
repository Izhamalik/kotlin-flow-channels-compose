package com.example.flowschannels.navigation

/**
 * One route per lesson. The dashboard is the table of contents; everything else is a chapter.
 */
object Destinations {
    const val DASHBOARD = "dashboard"

    const val FLOW_BASICS = "flow_basics"
    const val COLD_FLOW = "cold_flow"
    const val FLOW_LIFECYCLE = "flow_lifecycle"
    const val OPERATORS = "operators"
    const val OPERATORS_PLAYGROUND = "operators_playground"
    const val COMBINING = "combining"
    const val COMBINE = "combine"
    const val ZIP = "zip"
    const val MERGE = "merge"
    const val FLATMAP = "flatmap"
    const val ERROR_HANDLING = "error_handling"
    const val CANCELLATION = "cancellation"

    const val HOT_FLOWS = "hot_flows"
    const val STATE_FLOW = "state_flow"
    const val UI_STATE = "ui_state"
    const val SHARED_FLOW = "shared_flow"
    const val STATE_VS_SHARED = "state_vs_shared"

    const val CHANNELS = "channels"
    const val CHANNEL_CAPACITY = "channel_capacity"
    const val FLOW_VS_CHANNEL = "flow_vs_channel"
    const val CHANNEL_VS_SHARED = "channel_vs_shared"
    const val RECEIVE_AS_FLOW = "receive_as_flow"

    const val CALLBACK_FLOW = "callback_flow"
    const val CHANNEL_FLOW = "channel_flow"
    const val FLOW_CONTEXT = "flow_context"
    const val BACKPRESSURE = "backpressure"

    const val FLOW_VIEWMODEL = "flow_viewmodel"
    const val FLOW_REPOSITORY = "flow_repository"
    const val FLOW_ROOM = "flow_room"
    const val FLOW_RETROFIT = "flow_retrofit"
    const val FLOW_COMPOSE = "flow_compose"
    const val SEARCH = "search"
    const val TESTING = "testing"
}

data class LessonEntry(
    val title: String,
    val subtitle: String,
    val route: String,
)

data class LessonGroup(
    val title: String,
    val description: String,
    val lessons: List<LessonEntry>,
)

val Curriculum: List<LessonGroup> = listOf(
    LessonGroup(
        title = "Flow basics",
        description = "What a Flow is, why it is cold, and when it actually starts.",
        lessons = listOf(
            LessonEntry("What is Flow?", "The stream mental model", Destinations.FLOW_BASICS),
            LessonEntry("Cold Flow", "Collection starts the work", Destinations.COLD_FLOW),
            LessonEntry("Flow lifecycle", "Create → collect → emit → complete / cancel", Destinations.FLOW_LIFECYCLE),
        ),
    ),
    LessonGroup(
        title = "Operators",
        description = "Transform, filter, combine, and flatten streams.",
        lessons = listOf(
            LessonEntry("Flow operators", "map, filter, take, debounce, and the rest", Destinations.OPERATORS),
            LessonEntry("Operators playground", "Pick an operator, watch input become output", Destinations.OPERATORS_PLAYGROUND),
            LessonEntry("Combining Flows", "combine vs zip vs merge", Destinations.COMBINING),
            LessonEntry("combine", "Re-emit whenever any source changes", Destinations.COMBINE),
            LessonEntry("zip", "Pair values by index", Destinations.ZIP),
            LessonEntry("merge", "Interleave without pairing", Destinations.MERGE),
            LessonEntry("flatMap operators", "Concat, merge, latest — with a search flavour", Destinations.FLATMAP),
        ),
    ),
    LessonGroup(
        title = "Errors & cancellation",
        description = "Exceptions are not cancellation. Collection is a coroutine.",
        lessons = listOf(
            LessonEntry("Error handling", "catch, retry, retryWhen, onStart, onCompletion", Destinations.ERROR_HANDLING),
            LessonEntry("Cancellation", "collectLatest, flatMapLatest, lifecycle, ViewModel", Destinations.CANCELLATION),
        ),
    ),
    LessonGroup(
        title = "Hot Flows",
        description = "StateFlow and SharedFlow: shared, running independently of a single collector.",
        lessons = listOf(
            LessonEntry("Hot Flows", "What 'hot' actually means", Destinations.HOT_FLOWS),
            LessonEntry("StateFlow", "The UI-state holder", Destinations.STATE_FLOW),
            LessonEntry("UI state pattern", "Loading / Success / Error as StateFlow", Destinations.UI_STATE),
            LessonEntry("SharedFlow", "Events: snackbar, toast, navigation", Destinations.SHARED_FLOW),
            LessonEntry("StateFlow vs SharedFlow", "State is not an event", Destinations.STATE_VS_SHARED),
        ),
    ),
    LessonGroup(
        title = "Channels",
        description = "Communication between coroutines, not a stream you share.",
        lessons = listOf(
            LessonEntry("Channels", "send, receive, close, trySend", Destinations.CHANNELS),
            LessonEntry("Channel capacity", "Rendezvous, buffered, unlimited, conflated", Destinations.CHANNEL_CAPACITY),
            LessonEntry("Flow vs Channel", "A stream vs a pipe", Destinations.FLOW_VS_CHANNEL),
            LessonEntry("Channel vs SharedFlow", "Point-to-point vs broadcast", Destinations.CHANNEL_VS_SHARED),
            LessonEntry("receiveAsFlow", "What changes when a Channel is a Flow", Destinations.RECEIVE_AS_FLOW),
        ),
    ),
    LessonGroup(
        title = "Advanced Flow",
        description = "Bridges, concurrency, context, and backpressure.",
        lessons = listOf(
            LessonEntry("callbackFlow", "Turn listeners into Flow", Destinations.CALLBACK_FLOW),
            LessonEntry("channelFlow", "Emit from several coroutines", Destinations.CHANNEL_FLOW),
            LessonEntry("Flow context", "flowOn, upstream vs downstream", Destinations.FLOW_CONTEXT),
            LessonEntry("Backpressure", "buffer, conflate, collectLatest, overflow", Destinations.BACKPRESSURE),
        ),
    ),
    LessonGroup(
        title = "Android & Compose",
        description = "Where Flow lives in a real app.",
        lessons = listOf(
            LessonEntry("Flow + ViewModel", "Compose → ViewModel → StateFlow → Repository", Destinations.FLOW_VIEWMODEL),
            LessonEntry("Flow + Repository", "When to return Flow vs suspend", Destinations.FLOW_REPOSITORY),
            LessonEntry("Flow + Room", "Reactive table observation", Destinations.FLOW_ROOM),
            LessonEntry("Flow + Retrofit", "suspend for one shot, Flow for a stream", Destinations.FLOW_RETROFIT),
            LessonEntry("Flow + Compose", "collectAsStateWithLifecycle", Destinations.FLOW_COMPOSE),
            LessonEntry("Search example", "debounce → distinctUntilChanged → flatMapLatest", Destinations.SEARCH),
            LessonEntry("Flow testing", "runTest, Turbine, virtual time", Destinations.TESTING),
        ),
    ),
)
