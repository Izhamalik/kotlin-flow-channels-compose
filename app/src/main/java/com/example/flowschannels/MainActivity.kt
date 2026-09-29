package com.example.flowschannels

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.flowschannels.navigation.AppNavHost
import com.example.flowschannels.ui.theme.FlowsChannelsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FlowsChannelsTheme {
                AppNavHost()
            }
        }
    }
}
