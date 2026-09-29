package com.example.flowschannels.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A top-to-bottom pipeline, used to draw the shape of a Flow chain:
 * `Room → Flow<List<User>> → Repository → ViewModel → StateFlow → Compose`.
 */
@Composable
fun PipelineDiagram(
    steps: List<String>,
    modifier: Modifier = Modifier,
    highlightIndices: Set<Int> = emptySet(),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        steps.forEachIndexed { index, step ->
            val highlighted = index in highlightIndices
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (highlighted) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Text(
                    text = step,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    color = if (highlighted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
            if (index != steps.lastIndex) {
                Text(
                    text = "↓",
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 1.dp),
                )
            }
        }
    }
}

/** One lane of a marble diagram: which column carries which value. */
data class TimelineRow(
    val label: String,
    val marbles: Map<Int, String>,
    val completesAt: Int? = null,
)

/**
 * A marble diagram. Time runs left to right in discrete columns.
 *
 * This is the clearest way to show the difference between `combine` (re-emits on *either* source),
 * `zip` (pairs strictly by index) and `merge` (interleaves without pairing).
 */
@Composable
fun MarbleDiagram(
    rows: List<TimelineRow>,
    columns: Int,
    modifier: Modifier = Modifier,
    caption: String? = null,
    outputRowIndex: Int = rows.lastIndex,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEachIndexed { rowIndex, row ->
            val isOutput = rowIndex == outputRowIndex
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.label,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = if (isOutput) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(58.dp),
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth()) {
                        repeat(columns) { column ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                val value = row.marbles[column]
                                when {
                                    value != null -> Marble(value, isOutput)
                                    row.completesAt == column -> Text(
                                        text = "|",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun Marble(value: String, emphasised: Boolean) {
    val background = if (emphasised) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val foreground = if (emphasised) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = if (value.length > 2) 8.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            color = foreground,
        )
    }
}

/** Dense comparison table used by every "X vs Y" lesson. */
@Composable
fun ComparisonTable(
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier,
    weights: List<Float> = List(headers.size) { 1f },
    minWidthPerColumn: Int = 150,
) {
    val totalWidth = (minWidthPerColumn * headers.size).dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .width(totalWidth)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(10.dp),
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                headers.forEachIndexed { index, header ->
                    TableCell(
                        text = header,
                        weight = weights.getOrElse(index) { 1f },
                        bold = true,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            rows.forEachIndexed { rowIndex, row ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (rowIndex % 2 == 0) {
                                Color.Transparent
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                        ),
                ) {
                    row.forEachIndexed { index, cell ->
                        TableCell(
                            text = cell,
                            weight = weights.getOrElse(index) { 1f },
                            bold = index == 0,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TableCell(
    text: String,
    weight: Float,
    bold: Boolean,
    color: Color,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        color = color,
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 10.dp, vertical = 9.dp),
    )
}

/** A compact two-column "before / after" or "A / B" side-by-side block. */
@Composable
fun SideBySide(
    leftTitle: String,
    leftBody: String,
    rightTitle: String,
    rightBody: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SideBySideCard(leftTitle, leftBody, Tone.Bad, Modifier.weight(1f))
        SideBySideCard(rightTitle, rightBody, Tone.Good, Modifier.weight(1f))
    }
}

@Composable
private fun SideBySideCard(title: String, body: String, tone: Tone, modifier: Modifier) {
    Surface(
        color = tone.container(),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = tone.onContainer(),
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = tone.onContainer(),
            )
        }
    }
}
