package com.example.flowschannels.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowschannels.core.LogKind
import com.example.flowschannels.core.LogLine

/**
 * The "Live Output" panel every lesson shares.
 *
 * Each line shows the milliseconds elapsed since the demo started. Timing is not decoration here:
 * it is the only way to *see* that `debounce` waits, that `conflate` skips, that `buffer` decouples
 * producer from collector, and that a RENDEZVOUS channel suspends the sender.
 */
@Composable
fun OutputConsole(
    lines: List<LogLine>,
    modifier: Modifier = Modifier,
    title: String = "Live output",
    height: Dp = 220.dp,
    onClear: (() -> Unit)? = null,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${lines.size} lines",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.weight(1f))
            if (onClear != null) {
                TextButton(onClick = onClear) { Text("Clear") }
            }
        }

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            if (lines.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Nothing collected yet — run the example above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    items(lines, key = { it.id }) { line -> ConsoleRow(line) }
                }
            }
        }
    }
}

@Composable
private fun ConsoleRow(line: LogLine) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = line.elapsedMs.toString().padStart(5) + "ms",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.width(10.dp))
        if (line.lane != null) {
            Text(
                text = line.lane.take(10).padEnd(10),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = line.text,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = if (line.kind == LogKind.Collect || line.kind == LogKind.Receive) {
                FontWeight.SemiBold
            } else {
                FontWeight.Normal
            },
            color = line.kind.color(),
        )
    }
}

@Composable
private fun LogKind.color(): Color = when (this) {
    LogKind.Info -> MaterialTheme.colorScheme.onSurfaceVariant
    LogKind.Emit -> MaterialTheme.colorScheme.primary
    LogKind.Collect -> MaterialTheme.colorScheme.tertiary
    LogKind.Transform -> MaterialTheme.colorScheme.secondary
    LogKind.Send -> MaterialTheme.colorScheme.primary
    LogKind.Receive -> MaterialTheme.colorScheme.tertiary
    LogKind.Lifecycle -> MaterialTheme.colorScheme.outline
    LogKind.Error -> MaterialTheme.colorScheme.error
    LogKind.Cancel -> MaterialTheme.colorScheme.error
}
