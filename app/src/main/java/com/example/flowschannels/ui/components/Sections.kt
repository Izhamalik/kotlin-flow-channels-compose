package com.example.flowschannels.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Tones used to colour-code advice blocks consistently across all lessons. */
enum class Tone { Neutral, Good, Caution, Bad, Highlight }

@Composable
fun Tone.container(): Color = when (this) {
    Tone.Neutral -> MaterialTheme.colorScheme.surfaceVariant
    Tone.Good -> MaterialTheme.colorScheme.secondaryContainer
    Tone.Caution -> MaterialTheme.colorScheme.tertiaryContainer
    Tone.Bad -> MaterialTheme.colorScheme.errorContainer
    Tone.Highlight -> MaterialTheme.colorScheme.primaryContainer
}

@Composable
fun Tone.onContainer(): Color = when (this) {
    Tone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    Tone.Good -> MaterialTheme.colorScheme.onSecondaryContainer
    Tone.Caution -> MaterialTheme.colorScheme.onTertiaryContainer
    Tone.Bad -> MaterialTheme.colorScheme.onErrorContainer
    Tone.Highlight -> MaterialTheme.colorScheme.onPrimaryContainer
}

/** A titled card. Every lesson is a vertical stack of these, always in the same order. */
@Composable
fun LessonSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(Modifier.size(8.dp))
        content()
    }
}

@Composable
fun Paragraph(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.trimIndent(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun BulletList(
    items: List<String>,
    modifier: Modifier = Modifier,
    bullet: String = "•",
    bulletColor: Color = MaterialTheme.colorScheme.primary,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { item ->
            Row {
                Text(
                    text = bullet,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = bulletColor,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** A short, strongly coloured "remember this" block. */
@Composable
fun Callout(
    text: String,
    tone: Tone = Tone.Highlight,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    Surface(
        color = tone.container(),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            if (label != null) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = tone.onContainer(),
                )
                Spacer(Modifier.size(4.dp))
            }
            Text(
                text = text.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                color = tone.onContainer(),
            )
        }
    }
}

/** Horizontal list of the APIs a lesson covers, so learners can scan a screen's surface area. */
@Composable
fun ApiChips(apis: List<String>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
    ) {
        items(apis) { api ->
            AssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Text(
                        text = api,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

/** A card used for the two "advice" blocks (when to use / when not to use / mistakes). */
@Composable
fun AdviceCard(
    title: String,
    items: List<String>,
    tone: Tone,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = tone.container()),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = tone.onContainer(),
            )
            HorizontalDivider(color = tone.onContainer().copy(alpha = 0.2f))
            items.forEach { item ->
                Row {
                    Text(
                        text = if (tone == Tone.Bad) "✗" else if (tone == Tone.Good) "✓" else "•",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = tone.onContainer(),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tone.onContainer(),
                    )
                }
            }
        }
    }
}
