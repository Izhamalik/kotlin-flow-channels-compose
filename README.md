# Kotlin Flow & Channels with Jetpack Compose

An interactive textbook: a Jetpack Compose app plus this README. The app is the course. Each lesson has an explanation, runnable code, a live console with timestamps, real-world use cases, and the usual "when not to use this" caveats.

**This is not a coroutines course.** You are expected to already know `launch`, `async`, `suspend`, scopes, dispatchers, and structured concurrency. Coroutines appear here only as the machinery that collects a Flow, sends on a Channel, or cancels work.

The jump this project is built for:

```text
"I know Coroutines but don't really understand Flow"
                    ↓
"I understand Flow, StateFlow, SharedFlow, Channels,
 their operators, lifecycle, cancellation, backpressure,
 and how to use them correctly in Jetpack Compose."
```

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [How to use the app](#how-to-use-the-app)
3. [What is Flow?](#what-is-flow)
4. [Cold Flow](#cold-flow)
5. [Hot Flow](#hot-flow)
6. [Flow Lifecycle](#flow-lifecycle)
7. [Flow Operators](#flow-operators)
8. [StateFlow](#stateflow)
9. [SharedFlow](#sharedflow)
10. [Channels](#channels)
11. [callbackFlow](#callbackflow)
12. [channelFlow](#channelflow)
13. [Cold Flow vs Hot Flow](#cold-flow-vs-hot-flow)
14. [Flow vs StateFlow vs SharedFlow vs Channel](#flow-vs-stateflow-vs-sharedflow-vs-channel)
15. [Backpressure](#backpressure)
16. [Flow + Jetpack Compose](#flow--jetpack-compose)
17. [Flow + ViewModel](#flow--viewmodel)
18. [Flow + Repository](#flow--repository)
19. [Flow + Room](#flow--room)
20. [Flow + Retrofit](#flow--retrofit)
21. [Real-World Use Cases](#real-world-use-cases)
22. [Common Mistakes](#common-mistakes)
23. [Interview Questions](#interview-questions)
24. [Cheat Sheet](#cheat-sheet)

## Prerequisites

- Kotlin coroutines (assumed).
- A recent Android Studio and the SDK for compileSdk 37 (required by the existing AndroidX versions in this project).
- No network: Room, Retrofit, location, and search all have offline fakes.

Open the app, pick a lesson, press **Run**, watch the timestamps. Then read the same idea here if you want the long form.

## How to use the app

```text
Flow & Channels Learning
│
├── Flow Basics / Cold Flow / Lifecycle
├── Flow Operators / Operators playground
├── Combining Flows / combine / zip / merge
├── FlatMap Operators
├── Error Handling / Cancellation
├── Hot Flows / StateFlow / UI state / SharedFlow / comparison
├── Channels / Capacity / Flow vs Channel / Channel vs SharedFlow / receiveAsFlow
├── callbackFlow / channelFlow / flowOn / Backpressure
├── Flow + ViewModel / Repository / Room / Retrofit / Compose
├── Search Example
└── Flow Testing
```

Unit tests live in `app/src/test` and are part of the course (`runTest`, Turbine, virtual time).

---

# What is Flow?

A **Flow** is a cold (by default), sequential, cancellable stream of values that can emit zero, one, or many times, then complete or fail.

```kotlin
val numbers: Flow<Int> = flow {
    emit(1)
    emit(2)
    emit(3)
}

viewModelScope.launch {
    numbers.collect { value -> println(value) }
}
```

Mental model:

```text
flow { emit(value) }
        ↓
   operators
        ↓
   collect { use(value) }
```

`flow { }` is a recipe. Nothing runs until `collect`. `emit` and `collect` are both suspend functions, which is why cancellation of the collecting coroutine stops the stream. That is the only coroutine fact you must keep in mind for the rest of this document.

Use a Flow when there is a **stream**. Use `suspend fun` when there is a **single result**.

---

# Cold Flow

Cold means **each collector starts an independent execution** of the producer.

```kotlin
val ticks = flow {
    println("started")
    emit(1); emit(2)
}

launch { ticks.collect { println("A $it") } }
launch { ticks.collect { println("B $it") } }
// prints "started" twice
```

Declaring the Flow does no I/O. Operators do not run until collect. Two collectors of `dao.observeUsers()` are two queries (Room's Flow is cold per collector; you share it later with `stateIn`).

This is the default for `flow { }`, `flowOf`, `callbackFlow`, `channelFlow`, and operator chains. Hot streams are the exception.

---

# Hot Flow

A Flow is **hot** when its producer is not 1:1 with a collector.

Three ways there:

1. Construct hot: `MutableStateFlow`, `MutableSharedFlow`
2. Convert cold: `stateIn`, `shareIn`
3. Rarely, an API that is already hot

`SharingStarted` decides when the upstream of `stateIn` / `shareIn` is allowed to run:

| Policy | Starts | Stops |
| --- | --- | --- |
| `Eagerly` | Immediately in the scope | When the scope dies |
| `Lazily` | First subscriber | Never (while the scope lives) |
| `WhileSubscribed(timeout)` | While anyone collects | `timeout` after the last subscriber leaves |

`WhileSubscribed(5_000)` is the ViewModel default: a configuration change does not restart Room; going to the background long enough does.

---

# Flow Lifecycle

```text
Create Flow     (idle)
     ↓
Collect
     ↓
Emit → Transform → Collect
     ↓
Complete / Cancel / Throw
```

- **Starts** at `collect` (cold) or when the sharing policy says so (hot).
- **Stops** when collect returns, the collecting Job is cancelled, or the upstream completes/fails.
- **Multiple collectors, cold:** independent lifetimes.
- **Multiple collectors, hot:** one upstream lifetime.

`onStart` can emit a loading value. `onCompletion` always runs (`cause == null` on success, `CancellationException` on cancel, otherwise failure).

`collectAsStateWithLifecycle()` is this diagram applied to `Lifecycle.State.STARTED`.

---

# Flow Operators

Intermediate operators return a new Flow and stay lazy. Terminal operators (`collect`, `first`, `reduce`, …) start collection.

For every operator: purpose, syntax, example, behavior, use case, common mistake.

### map

**Purpose.** One output per input, transformed.

```kotlin
flowOf(1, 2, 3).map { it * 2 }  // 2, 4, 6
```

**Behavior.** Preserves timing. Exceptions in the lambda fail the stream.

**Use case.** DTO → domain.

**Mistake.** Heavy work in `map` on Main. Put `flowOn(IO)` *above* the expensive operator.

### filter

**Purpose.** Keep values that match.

```kotlin
flowOf(1, 2, 3, 4).filter { it % 2 == 0 }  // 2, 4
```

**Behavior.** Skipped values still cost whatever the upstream did.

**Use case.** Ignore blank search queries.

**Mistake.** Filtering in Compose after the repository already did expensive work.

### transform

**Purpose.** Generalised `map`: 0..N emits per input.

```kotlin
flowOf(1, 2).transform { emit(it); emit("$it!") }
```

**Behavior.** The lambda is suspending (`delay`, `emitAll`).

**Use case.** Cached value then refresh.

**Mistake.** Reinventing `transform` with `map` + `flattenMerge`.

### onEach

**Purpose.** Side effect, then forward the same value. Does not start collection.

```kotlin
flow.onEach { Log.d("flow", "$it") }.collect { … }
```

**Use case.** Analytics, debug logs.

**Mistake.** Treating `onEach` as `collect`, or collecting inside `onEach`.

### take / takeWhile

**Purpose.** Complete after N values, or when a predicate fails. Cancels upstream.

```kotlin
flowOf(1, 2, 3, 4, 5).take(3)           // 1, 2, 3
flowOf(1, 2, 3, 4).takeWhile { it < 4 } // 1, 2, 3
```

**Use case.** Tests, "first three GPS fixes".

**Mistake.** `take(1)` on a `StateFlow` — it never completes if you expected completion after the replay.

### drop

**Purpose.** Skip the first N values.

```kotlin
flowOf(1, 2, 3, 4, 5).drop(2)  // 3, 4, 5
```

**Use case.** Ignore an initial empty Room emission you already handle with `onStart`.

**Mistake.** Confusing with `filter` (positional vs by value).

### distinctUntilChanged

**Purpose.** Skip a value if it equals the *previous* one.

```kotlin
flowOf(1, 1, 2, 2, 3, 1).distinctUntilChanged()  // 1, 2, 3, 1
```

**Behavior.** Consecutive only. Non-consecutive repeats pass.

**Use case.** Search queries, StateFlow UI.

**Mistake.** Expecting global uniqueness (`distinct()` is a different, buffering operator).

### debounce

**Purpose.** Emit only after the upstream stays quiet for a timeout.

```kotlin
queryFlow.debounce(300)
```

**Behavior.** Burst `K, Ko, Kot` → last one after 300 ms of quiet. Uses delays; tests must use virtual time.

**Use case.** Typeahead.

**Mistake.** Debouncing with a `Handler` in the `TextField` instead of the query Flow.

### sample

**Purpose.** Emit the latest value every period; drop intermediates.

```kotlin
ticks.sample(500)
```

**Use case.** High-frequency sensors downsampled for UI.

**Mistake.** Using `sample` for events that must not be lost.

### collectLatest

**Purpose.** Terminal: a new value cancels the previous collector lambda.

```kotlin
flow.collectLatest { value ->
    renderSlow(value) // cancelled if a newer value arrives
}
```

**Use case.** Apply only the latest search result.

**Mistake.** Collecting UI state you must paint on every emission — you will skip frames. There is also `mapLatest` for a Flow-returning form.

### first / firstOrNull / single

**Purpose.** Terminal. `first` takes one and cancels the rest. `single` requires exactly one and then completion.

**Use case.** `first()` on a Room Flow when you need a snapshot.

**Mistake.** `single()` on `StateFlow` (never completes). `first()` on an infinite stream without timeout.

### reduce / fold / scan / runningFold

**Purpose.** Aggregations. `reduce`/`fold` are terminal. `scan`/`runningFold` emit each intermediate.

```kotlin
flowOf(1, 2, 3).scan(0) { acc, n -> acc + n }  // 0, 1, 3, 6
```

**Use case.** Running totals, download progress.

**Mistake.** Using `reduce` on an empty Flow — it throws.

### combine

**Purpose.** Snapshot of the latest value of each source. Emits when *any* source emits, after every source has emitted at least once.

```kotlin
combine(nameFlow, ageFlow) { name, age -> "$name - $age" }
```

**Use case.** Form validity, Room list + network banner.

**Mistake.** Wondering why nothing appears — one source has not emitted yet. Seed with `onStart` / `stateIn`.

### zip

**Purpose.** Pair by index. Extra values wait. Completes when either side completes.

```kotlin
nameFlow.zip(ageFlow) { name, age -> "$name - $age" }
```

**Use case.** Aligned records, tests.

**Mistake.** Zipping UI fields. A late name stalls the age field forever.

### merge

**Purpose.** Interleave. No pairing. Types share a supertype.

```kotlin
merge(clicks, deepLinks)
```

**Use case.** Homogeneous events from several sources.

**Mistake.** Merging name and age as `Flow<Any>` and parsing in the UI. That is `combine`.

### flatMapConcat / flatMapMerge / flatMapLatest

Each outer value starts an inner Flow. Policy for in-flight inners:

| Operator | Inners | Stale work |
| --- | --- | --- |
| `flatMapConcat` | One at a time, ordered | Runs to completion |
| `flatMapMerge` | Concurrent (optional cap) | All complete, order not guaranteed |
| `flatMapLatest` | Only the latest | Previous cancelled |

**Use case.** Latest: search. Merge: N downloads. Concat: serial writes.

**Mistake.** `flatMapConcat` on a search box. `flatMapMerge` showing mixed `K` and `Kotlin` results.

### catch / retry / retryWhen

**Purpose.** Recover or resubscribe.

```kotlin
upstream
    .retryWhen { cause, attempt -> cause is IOException && attempt < 3 }
    .catch { emit(emptyList()) }
```

**Behavior.** `catch` sees *upstream* exceptions only. `retry` collects the cold upstream again. **`CancellationException` is not an error** — `catch` does not swallow it.

**Mistake.** Empty `catch { }`. Retrying cancellation. Putting `catch` *above* the operator that throws.

### onStart / onCompletion

**Purpose.** Hooks around collection.

```kotlin
flow.onStart { emit(emptyList()) }
    .onCompletion { cause -> log(cause) }
```

**Mistake.** Thinking `onCompletion` skips cancellation — it runs, with a cause.

### flowOn

**Purpose.** Switch the context of **everything above** the operator. Downstream stays on the collector's context.

```kotlin
repository.observeUsers()
    .flowOn(Dispatchers.IO)
    .map { toUi(it) }  // collector context, usually Main
```

**Mistake.** Believing `collect` on Main moves Room to Main. Putting `flowOn` *below* the mapper you wanted on IO.

### buffer / conflate

**Purpose.** Backpressure. Default Flow is rendezvous (`emit` waits for `collect`).

- `buffer` — queue, then suspend when full
- `conflate` — keep only the latest unread

**Mistake.** `buffer` unbounded as a "performance fix". `conflate` on snackbars.

---

# StateFlow

Hot, conflated, always has a value, never completes.

```kotlin
class CounterViewModel : ViewModel() {
    private val _count = MutableStateFlow(0)
    val count = _count.asStateFlow()

    fun increment() {
        _count.update { it + 1 }
    }
}

// Compose
val count by viewModel.count.collectAsStateWithLifecycle()
```

- `.value` is the current snapshot.
- `update { }` is atomic when the next state depends on the previous.
- `asStateFlow()` is the encapsulation boundary — never expose `MutableStateFlow`.
- `stateIn` converts a cold Flow, with an `initialValue` for Compose to render first.
- Equal consecutive values (`Any.equals`) are dropped. Use data classes.

`collectAsStateWithLifecycle()` is preferred for UI state: it unsubscribes in `STOPPED`, which is how `WhileSubscribed` is meant to work.

---

# SharedFlow

Hot broadcast with configurable `replay`, `extraBufferCapacity`, and `onBufferOverflow`. No required initial value.

```kotlin
private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
val events = _events.asSharedFlow()
```

- `emit` suspends (overflow `SUSPEND`).
- `tryEmit` never waits; returns false if it cannot deliver.
- `replay = 0` is the UI-event default. `replay = 1` is how you accidentally resurrect a snackbar after rotation.
- Multiple collectors each receive the value (broadcast). Opposite of a Channel.

**State vs event:** state is what is true now (replay it). An event happened once (do not replay it). Collect events in `LaunchedEffect`, not with `collectAsStateWithLifecycle`.

Zero buffer + `tryEmit` from a click handler silently drops. Give events a small extra buffer for the configuration-change gap.

---

# Channels

A **Channel** is a pipe between coroutines, not a stream you subscribe to.

```kotlin
private val channel = Channel<Int>()

suspend fun sendValue(value: Int) {
    channel.send(value)
}

viewModelScope.launch {
    for (value in channel) {
        println(value)
    }
}
```

- `send` / `receive` suspend. `trySend` / `tryReceive` do not.
- `close()` signals no more values; the `for` loop then ends.
- Each element is consumed by **one** receiver (point-to-point).

### Capacity

| Kind | Code | Behavior |
| --- | --- | --- |
| Rendezvous | `Channel(Channel.RENDEZVOUS)` | No buffer. `send` waits for `receive`. |
| Buffered | `Channel(capacity = 10)` | `send` waits when full. Bounded backpressure. |
| Unlimited | `Channel(Channel.UNLIMITED)` | `send` never waits. Memory risk. |
| Conflated | `Channel(Channel.CONFLATED)` | Keep latest unread. Drop intermediates. |

`receiveAsFlow()` exposes operators but **does not broadcast**. Two collectors race on `receive`. `consumeAsFlow()` also cancels the Channel when the collector cancels.

---

# callbackFlow

Turns a listener API into a **cold** Flow.

```kotlin
fun FakeLocationService.locationUpdates(): Flow<Location> = callbackFlow {
    val callback = object : LocationCallback {
        override fun onLocationChanged(location: Location) {
            trySend(location) // not send — callbacks are not coroutines
        }
        override fun onFailure(error: Throwable) {
            close(error)
        }
    }
    registerCallback(callback)
    awaitClose { unregisterCallback(callback) }
}
```

**Why it exists.** GPS, sensors, WebSockets, Firebase, `BroadcastReceiver` push into callbacks. None of them are Flow.

**`awaitClose` is mandatory.** Without it the builder returns, collection completes immediately, and the listener leaks. Cancellation of `collect` runs `awaitClose`.

---

# channelFlow

A Flow builder that allows **concurrent senders**.

```kotlin
fun load(): Flow<String> = channelFlow {
    launch { send(fetchA()) }
    launch { send(fetchB()) }
}
```

`flow { }` is sequential; nested `launch` cannot call `emit`. `channelFlow { }` is a `ProducerScope`. It is still cold: two collectors start two fan-outs.

`callbackFlow` is the callback-oriented cousin that requires `awaitClose`.

---

# Cold Flow vs Hot Flow

| | Cold | Hot |
| --- | --- | --- |
| Producer vs collector | 1:1 | 1:N (shared) |
| Starts | On each `collect` | Sharing policy / construction |
| Replay of past | Full independent run | StateFlow: current. SharedFlow: `replay` |
| Typical | Room query, `flow { }`, `callbackFlow` | `StateFlow`, `SharedFlow`, `stateIn` |
| Two collectors | Two executions | One execution, two subscribers |

Stay cold until starting the producer once per collector is wrong or expensive. Do not `stateIn` "for consistency".

---

# Flow vs StateFlow vs SharedFlow vs Channel

| | Flow (cold) | StateFlow | SharedFlow | Channel |
| --- | --- | --- | --- | --- |
| **Purpose** | Stream recipe | Current UI/domain state | Broadcast of occurrences | Pipe between coroutines |
| **Hot/cold** | Cold | Hot | Hot | Neither (rendezvous/queue) |
| **Current value** | No | Yes (`.value`) | No (unless you fake it with replay) | Head of queue, if any |
| **Replay** | Full run per collector | Latest | Configurable 0..N | Queued items wait for a receiver |
| **Buffering** | `buffer` / `conflate` | Conflates equals | `extraBufferCapacity` + overflow | `capacity` |
| **Multiple collectors** | Independent runs | All see current + updates | Broadcast | One receiver per element |
| **Delivery** | Subscribe | State | Multicast events | Point-to-point |
| **Typical use** | Repository observation, operators | Screen state | Snackbar, nav | Work queue, fan-in |
| **Cancellation** | Stops that collect | Collectors leave; value remains | Collectors leave | Cancels that send/receive |
| **Compose** | Convert with `stateIn` first | `collectAsStateWithLifecycle` | `LaunchedEffect` + `collect` | Rarely in UI; convert or don't |

Do not oversimplify to "always StateFlow". A snackbar in StateFlow replays. A current user in SharedFlow(replay=0) is missing after process death until the next event. A Channel as a UI event bus steals events when a second collector appears.

---

# Backpressure

When the producer is faster than the consumer:

```text
Producer → 1 2 3 4 5 6 → Slow consumer
```

| Tool | Policy |
| --- | --- |
| Default `emit` | Suspend until collect finishes (rendezvous) |
| `buffer` | Queue, then suspend |
| `conflate` | Drop unread intermediates |
| `collectLatest` | Cancel slow work, start the new value |
| Channel capacity | Same ideas as buffer / conflate / rendezvous / unlimited |
| SharedFlow overflow | `SUSPEND`, `DROP_OLDEST`, `DROP_LATEST` |

Do not conflate billing events. Do not unlimited-buffer a firehose.

---

# Flow + Jetpack Compose

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

LaunchedEffect(viewModel) {
    viewModel.events.collect { snackbarHost.showSnackbar(it.message) }
}
```

- **State:** `StateFlow` + `collectAsStateWithLifecycle()`. Collection is active at `STARTED`.
- **Events:** `SharedFlow` + a one-shot collector. Do not collect events as state.
- Never call `collect` in the composable body (restarts every recomposition).
- Prefer lifecycle-aware collection over `collectAsState()` on Android.

---

# Flow + ViewModel

```text
Compose UI
    ↓
ViewModel
    ↓
StateFlow
    ↓
Repository
    ↓
Data source
```

```kotlin
val users: StateFlow<List<User>> = repository.observeUsers()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

fun refresh() {
    viewModelScope.launch { repository.refreshUsers() }
}
```

`viewModelScope` is used because its cancellation is the ViewModel's lifetime — not because this course teaches `launch`. Commands stay `suspend`. Observation stays `Flow` → `StateFlow`. Keep mutables private.

---

# Flow + Repository

```kotlin
interface UserRepository {
    fun observeUsers(): Flow<List<User>>
    suspend fun refreshUsers()
}
```

Expose Flow when the caller wants **future values**. Keep one-shot work as `suspend`. Wrapping `getUsers()` in `flow { emit(api()) }` for a single collect is usually noise. `DefaultUserRepository` in this project is the working example (`flowOn` at the edge, writes that make observers emit).

---

# Flow + Room

```kotlin
@Query("SELECT * FROM users")
fun observeUsers(): Flow<List<User>>
```

```text
Room → Flow<List<User>> → Repository → ViewModel StateFlow → Compose
```

Room re-runs the query when the table is invalidated. No polling. This app uses `InMemoryUserDao` with the same contract so the project stays free of an annotation processor. Writes are still `suspend`.

---

# Flow + Retrofit

A single HTTP call is:

```kotlin
@GET("users")
suspend fun getUsers(): List<UserDto>
```

That is the honest API. Retrofit *can* return Flow; that does not make every method a stream.

Flow around network work is useful for polling, `retryWhen` with backoff, `combine(local, flow { emit(api()) })`, and cancellation under `flatMapLatest`. `UsersApi` in this project is a real Retrofit interface implemented by `FakeUsersApi`.

---

# Real-World Use Cases

| Problem | Shape |
| --- | --- |
| Search | `debounce` → `distinctUntilChanged` → `flatMapLatest` → `StateFlow` |
| Form validation | `combine` of field Flows |
| Database observation | Room `Flow` → `stateIn` |
| Authentication state | `StateFlow<User?>` |
| Network status | `callbackFlow` or `stateIn` of a connectivity source |
| Pagination | `suspend` page loads, or a Flow of paging data from the paging library |
| Download progress | `StateFlow<Int>` or `scan` |
| Snackbar / navigation | `SharedFlow` events |
| Sensors / location | `callbackFlow` + lifecycle collection |
| WebSockets | `callbackFlow` / `channelFlow`, often `shareIn` |
| Local + remote | Room Flow `combine` a one-shot refresh, or write-through cache |

The **Search** lesson is the full pipeline, offline.

---

# Common Mistakes

1. Exposing `MutableStateFlow` / `MutableSharedFlow` publicly.
2. Collecting without lifecycle awareness (`collectAsState()`, `GlobalScope`).
3. Misusing SharedFlow as state (`replay = 0` and then asking "where's my user?").
4. Misusing Channel for broadcast UI events (second collector steals them).
5. Ignoring cancellation — leaking GPS because `awaitClose` was forgotten.
6. Swallowing `CancellationException` in `try/catch` around `emit`.
7. Incorrect `flowOn` placement (below the work you meant to move).
8. Overusing Flow: `fun login(): Flow<User>` that emits once.
9. Creating unnecessary hot streams (`Eagerly` for a rare screen).
10. Forgetting `awaitClose` in `callbackFlow`.
11. `Channel.UNLIMITED` as a default.
12. Misunderstanding `replay` (snackbar after rotation).
13. Misunderstanding `collectLatest` (skipping UI frames you needed).
14. Misunderstanding `flatMapLatest` vs `flatMapConcat` on search.
15. `catch { }` empty — failures look like an idle UI.
16. Collecting a new Flow instance every recomposition.
17. Collecting events with `collectAsStateWithLifecycle`.

---

# Interview Questions

Answers are short on purpose. Expand them from the lessons.

**1. What is a Flow?**  
A cold (by default) stream that emits values over time. Collection is a coroutine.

**2. Why is Flow cold by default?**  
So work exists only while someone listens, and each collector gets an independent run.

**3. What starts a cold Flow?**  
`collect` (or another terminal).

**4. Two collectors of the same cold Flow — what happens?**  
Two executions of the producer.

**5. What makes a Flow hot?**  
A producer that is not 1:1 with collectors: `StateFlow`, `SharedFlow`, `stateIn`, `shareIn`.

**6. StateFlow vs SharedFlow in one sentence?**  
StateFlow is current state with a required value; SharedFlow is configurable broadcast, usually events with `replay = 0`.

**7. Why does StateFlow require an initial value?**  
Someone may read `.value` or collect before the upstream has produced. UI always needs something to render.

**8. Why `update { }` instead of `value = value + 1`?**  
Atomic read-modify-write when several coroutines mutate.

**9. Why `asStateFlow()`?**  
So the UI cannot emit. Encapsulation of the hot holder.

**10. What does `WhileSubscribed(5000)` buy you?**  
Upstream keeps running across short collector gaps (rotation) and stops after 5s with no UI.

**11. Why `collectAsStateWithLifecycle`?**  
Collection follows `STARTED`. Matches `WhileSubscribed` and pauses Room/sensors off-screen.

**12. When is SharedFlow the wrong type for UI state?**  
When a late collector must see the current value. That is StateFlow.

**13. `emit` vs `tryEmit`?**  
`emit` suspends for buffer space. `tryEmit` returns false instead of waiting.

**14. What is `replay`?**  
How many past values a new subscriber is told. `0` for events. `1` is StateFlow's world.

**15. Channel vs SharedFlow for events?**  
Channel is point-to-point (one consumer per element). SharedFlow broadcasts. `receiveAsFlow()` does not change that.

**16. What is Channel capacity?**  
The backpressure policy: rendezvous, bounded buffer, unlimited, conflated.

**17. What happens on a RENDEZVOUS channel without a receiver?**  
`send` suspends. `trySend` fails.

**18. CONFLATED vs `conflate()` on Flow?**  
Same idea: keep the latest unread, drop intermediates.

**19. Why `callbackFlow` instead of `flow { }`?**  
Callbacks are not suspend functions; you need `trySend` and a guaranteed unregister (`awaitClose`).

**20. What if you omit `awaitClose`?**  
The builder can complete immediately; the listener leaks; you collect nothing useful.

**21. `flow { }` vs `channelFlow { }`?**  
Sequential single emitter vs concurrent `send` from child coroutines. Both cold.

**22. What does `flowOn` affect?**  
Upstream only (producer and operators above it).

**23. `combine` vs `zip` vs `merge`?**  
Latest-of-each vs pair-by-index vs interleave with no pairing.

**24. Why does `combine` emit nothing at first?**  
It waits until every source has emitted at least once.

**25. `flatMapLatest` vs `collectLatest`?**  
Both cancel previous work. `flatMapLatest` is an operator returning a Flow; `collectLatest` is a terminal.

**26. Why search uses debounce + distinctUntilChanged + flatMapLatest?**  
Drop keystrokes, drop duplicate queries, cancel in-flight searches.

**27. Does `catch` handle cancellation?**  
No. `CancellationException` is how collect stops. Do not swallow it.

**28. `retry` on a cold Flow does what?**  
Collects the upstream again from scratch.

**29. Room `Flow` vs `suspend` DAO?**  
Flow for observation that should update the UI. Suspend for a one-shot query/write.

**30. Should Retrofit return Flow?**  
Not by default. `suspend` for one response. Flow when you have a real stream (poll, combine, cancelable typeahead).

**31. Why not put snackbars in `UiState`?**  
They replay as state (rotation, recollect). They are events.

**32. What is backpressure in Flow?**  
The producer slowing down or dropping because the consumer is slower: default suspend, `buffer`, `conflate`, `collectLatest`.

**33. `stateIn` vs collecting in `init`?**  
`stateIn` is the sharing API with an explicit policy and initial value. A raw `collect` in `init` is easy to get wrong (errors, double collection).

**34. Multiple collectors of `receiveAsFlow()`?**  
They share one Channel. Values are load-balanced, not copied.

**35. How do you test `debounce`?**  
`runTest` + virtual time (`advanceTimeBy`) or Turbine. Do not `Thread.sleep`.

---

# Cheat Sheet

```text
Flow            cold stream recipe          collect to run
StateFlow       hot state, .value           UI state
SharedFlow      hot broadcast               events (replay = 0)
Channel         pipe, one consumer/item     queues, actors

collect         terminal, 1:1 with coroutine
collectLatest   cancel previous lambda

stateIn         cold Flow → StateFlow
shareIn         cold Flow → SharedFlow

map / filter / transform / onEach
take / drop / distinctUntilChanged
debounce / sample
combine / zip / merge
flatMapConcat / flatMapMerge / flatMapLatest

catch / retry / retryWhen
onStart / onCompletion
flowOn          upstream context
buffer          queue
conflate        latest unread

callbackFlow    listeners → Flow   (trySend, awaitClose)
channelFlow     concurrent senders
receiveAsFlow   Channel as Flow    (still point-to-point)

Compose         collectAsStateWithLifecycle for state
                LaunchedEffect + collect for events
```

## Tests

```bash
./gradlew testDebugUnitTest
```

Read `app/src/test/java/com/example/flowschannels/flow/` — the tests are named after the APIs they prove.
