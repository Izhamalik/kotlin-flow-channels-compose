package com.example.flowschannels.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flowschannels.core.DemoViewModel
import com.example.flowschannels.core.LogLine

@Composable
fun rememberDemoViewModel(): DemoViewModel = viewModel()

@Composable
fun DemoViewModel.collectLines(): List<LogLine> {
    val lines by log.lines.collectAsStateWithLifecycle()
    return lines
}
