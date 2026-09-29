package com.example.flowschannels.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class CodeSample(val code: String, val title: String? = null)

/**
 * The teaching payload of a lesson, kept separate from its interactive demo.
 *
 * Every screen in the app fills in the same fields, so the learner always finds the explanation,
 * the runnable example, the caveats and the mistakes in the same place.
 */
data class LessonDoc(
    val tagline: String,
    val whatIsIt: String,
    val keyApis: List<String> = emptyList(),
    val code: List<CodeSample> = emptyList(),
    val howItWorks: String = "",
    val useCases: List<String> = emptyList(),
    val whenToUse: List<String> = emptyList(),
    val whenNotToUse: List<String> = emptyList(),
    val mistakes: List<String> = emptyList(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    title: String,
    doc: LessonDoc,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    visuals: (@Composable ColumnScope.() -> Unit)? = null,
    demo: (@Composable ColumnScope.() -> Unit)? = null,
    extras: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        },
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Callout(text = doc.tagline, tone = Tone.Highlight, label = "In one sentence")

            LessonSection(title = "What is it?") {
                Paragraph(doc.whatIsIt)
            }

            if (doc.keyApis.isNotEmpty()) {
                LessonSection(title = "APIs covered here") {
                    ApiChips(doc.keyApis)
                }
            }

            if (visuals != null) {
                LessonSection(title = "Mental model") { visuals() }
            }

            if (doc.code.isNotEmpty()) {
                LessonSection(title = "Code example") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        doc.code.forEach { sample ->
                            CodeBlock(code = sample.code, title = sample.title)
                        }
                    }
                }
            }

            if (demo != null) {
                LessonSection(
                    title = "Run it",
                    subtitle = "Press a button and watch the timestamps in the console.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { demo() }
                }
            }

            if (doc.howItWorks.isNotBlank()) {
                LessonSection(title = "How it works") {
                    Paragraph(doc.howItWorks)
                }
            }

            if (extras != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) { extras() }
            }

            if (doc.useCases.isNotEmpty()) {
                LessonSection(title = "Real-world use cases") {
                    BulletList(doc.useCases)
                }
            }

            if (doc.whenToUse.isNotEmpty()) {
                AdviceCard(title = "When to use it", items = doc.whenToUse, tone = Tone.Good)
            }

            if (doc.whenNotToUse.isNotEmpty()) {
                AdviceCard(title = "When NOT to use it", items = doc.whenNotToUse, tone = Tone.Caution)
            }

            if (doc.mistakes.isNotEmpty()) {
                AdviceCard(title = "Common mistakes", items = doc.mistakes, tone = Tone.Bad)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = "End of lesson",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.size(24.dp))
        }
    }
}
