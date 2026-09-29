package com.example.flowschannels.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A read-only Kotlin snippet with lightweight syntax highlighting.
 *
 * Flow/Channel API names get their own colour so the eye can immediately find `collectLatest`,
 * `flatMapLatest`, `stateIn`, `awaitClose` and friends inside a longer snippet.
 */
@Composable
fun CodeBlock(
    code: String,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val colors = rememberCodeColors()
    val highlighted = remember(code, colors) { highlightKotlin(code.trimIndent(), colors) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.background,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.comment,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Text(
                text = highlighted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                color = colors.plain,
                softWrap = false,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            )
        }
    }
}

data class CodeColors(
    val background: Color,
    val plain: Color,
    val keyword: Color,
    val api: Color,
    val type: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val annotation: Color,
)

@Composable
private fun rememberCodeColors(): CodeColors = if (isSystemInDarkTheme()) DarkCodeColors else LightCodeColors

private val DarkCodeColors = CodeColors(
    background = Color(0xFF1B1B21),
    plain = Color(0xFFE3E1E6),
    keyword = Color(0xFFCF9BF0),
    api = Color(0xFF7FD1C1),
    type = Color(0xFFF2C97D),
    string = Color(0xFF9DD68B),
    number = Color(0xFFF29191),
    comment = Color(0xFF8A8A94),
    annotation = Color(0xFF8CB4F8),
)

private val LightCodeColors = CodeColors(
    background = Color(0xFFF3F1F7),
    plain = Color(0xFF1F1F24),
    keyword = Color(0xFF7B2FA8),
    api = Color(0xFF00695C),
    type = Color(0xFF8A5100),
    string = Color(0xFF2E6B26),
    number = Color(0xFFA32B2B),
    comment = Color(0xFF6C6C75),
    annotation = Color(0xFF1B4FA8),
)

private val KotlinKeywords = setOf(
    "as", "break", "by", "catch", "class", "companion", "const", "continue", "crossinline", "data",
    "do", "else", "enum", "false", "finally", "for", "fun", "get", "if", "import", "in", "infix",
    "init", "inline", "interface", "internal", "is", "it", "lateinit", "null", "object", "open",
    "operator", "out", "override", "package", "private", "protected", "public", "reified", "return",
    "sealed", "set", "super", "suspend", "this", "throw", "true", "try", "typealias", "val", "var",
    "vararg", "when", "where", "while", "yield",
)

/** API names worth spotlighting: everything a learner is here to recognise. */
private val FlowApis = setOf(
    "flow", "flowOf", "asFlow", "emit", "emitAll", "collect", "collectLatest", "collectIndexed",
    "collectAsState", "collectAsStateWithLifecycle", "launchIn", "onEach", "map", "mapLatest",
    "mapNotNull", "filter", "filterNot", "filterNotNull", "filterIsInstance", "transform",
    "transformLatest", "take", "takeWhile", "drop", "dropWhile", "distinctUntilChanged",
    "distinctUntilChangedBy", "debounce", "sample", "first", "firstOrNull", "last", "lastOrNull",
    "single", "singleOrNull", "toList", "toSet", "count", "reduce", "fold", "runningFold",
    "runningReduce", "scan", "combine", "combineTransform", "zip", "merge", "flattenConcat",
    "flattenMerge", "flatMapConcat", "flatMapMerge", "flatMapLatest", "catch", "retry", "retryWhen",
    "onStart", "onCompletion", "onEmpty", "flowOn", "buffer", "conflate", "cancellable",
    "shareIn", "stateIn", "asStateFlow", "asSharedFlow", "update", "updateAndGet", "getAndUpdate",
    "compareAndSet", "tryEmit", "resetReplayCache", "subscriptionCount", "value",
    "callbackFlow", "channelFlow", "awaitClose", "trySend", "trySendBlocking", "send", "receive",
    "tryReceive", "receiveAsFlow", "consumeAsFlow", "consumeEach", "close", "produce", "produceIn",
    "isClosedForSend", "isClosedForReceive", "onBufferOverflow", "launch", "async", "await",
    "runTest", "advanceTimeBy", "advanceUntilIdle", "runCurrent", "test", "awaitItem",
    "awaitComplete", "awaitError", "cancelAndIgnoreRemainingEvents", "turbineScope",
    "viewModelScope", "lifecycleScope", "repeatOnLifecycle", "withContext", "coroutineScope",
    "supervisorScope", "delay", "yield", "ensureActive", "isActive", "cancel", "cancelChildren",
    "Flow", "MutableStateFlow", "StateFlow", "MutableSharedFlow", "SharedFlow", "Channel",
    "SharingStarted", "WhileSubscribed", "Eagerly", "Lazily", "BufferOverflow",
)

private val TokenRegex = Regex(
    """(//[^\n]*|/\*[\s\S]*?\*/)""" +           // 1: comment
        """|("{3}[\s\S]*?"{3}|"(?:\\.|[^"\\\n])*")""" + // 2: string
        """|(@[A-Za-z_][A-Za-z0-9_]*)""" +      // 3: annotation
        """|(\b\d[\d_.]*[a-zA-Z]*\b)""" +       // 4: number
        """|([A-Za-z_][A-Za-z0-9_]*)""",        // 5: word
)

private fun highlightKotlin(code: String, colors: CodeColors): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    for (match in TokenRegex.findAll(code)) {
        if (match.range.first > cursor) append(code.substring(cursor, match.range.first))
        val text = match.value
        val style = when {
            match.groups[1] != null -> SpanStyle(color = colors.comment)
            match.groups[2] != null -> SpanStyle(color = colors.string)
            match.groups[3] != null -> SpanStyle(color = colors.annotation)
            match.groups[4] != null -> SpanStyle(color = colors.number)
            text in KotlinKeywords -> SpanStyle(color = colors.keyword, fontWeight = FontWeight.Medium)
            text in FlowApis -> SpanStyle(color = colors.api, fontWeight = FontWeight.Medium)
            text.first().isUpperCase() -> SpanStyle(color = colors.type)
            else -> null
        }
        if (style == null) append(text) else withStyle(style) { append(text) }
        cursor = match.range.last + 1
    }
    if (cursor < code.length) append(code.substring(cursor))
}
