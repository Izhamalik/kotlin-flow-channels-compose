package com.example.flowschannels.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.flowschannels.ui.dashboard.DashboardScreen
import com.example.flowschannels.ui.lessons.advanced.BackpressureScreen
import com.example.flowschannels.ui.lessons.advanced.CallbackFlowScreen
import com.example.flowschannels.ui.lessons.advanced.ChannelFlowScreen
import com.example.flowschannels.ui.lessons.advanced.FlowContextScreen
import com.example.flowschannels.ui.lessons.android.ComposeFlowScreen
import com.example.flowschannels.ui.lessons.android.RepositoryScreen
import com.example.flowschannels.ui.lessons.android.RetrofitScreen
import com.example.flowschannels.ui.lessons.android.RoomScreen
import com.example.flowschannels.ui.lessons.android.ViewModelFlowScreen
import com.example.flowschannels.ui.lessons.cancellation.CancellationScreen
import com.example.flowschannels.ui.lessons.channels.ChannelCapacityScreen
import com.example.flowschannels.ui.lessons.channels.ChannelVsSharedFlowScreen
import com.example.flowschannels.ui.lessons.channels.ChannelsScreen
import com.example.flowschannels.ui.lessons.channels.FlowVsChannelScreen
import com.example.flowschannels.ui.lessons.channels.ReceiveAsFlowScreen
import com.example.flowschannels.ui.lessons.combining.CombineScreen
import com.example.flowschannels.ui.lessons.combining.CombiningOverviewScreen
import com.example.flowschannels.ui.lessons.combining.MergeScreen
import com.example.flowschannels.ui.lessons.combining.ZipScreen
import com.example.flowschannels.ui.lessons.errors.ErrorHandlingScreen
import com.example.flowschannels.ui.lessons.flatmap.FlatMapScreen
import com.example.flowschannels.ui.lessons.flow.ColdFlowScreen
import com.example.flowschannels.ui.lessons.flow.FlowBasicsScreen
import com.example.flowschannels.ui.lessons.flow.FlowLifecycleScreen
import com.example.flowschannels.ui.lessons.hot.HotFlowsScreen
import com.example.flowschannels.ui.lessons.hot.SharedFlowScreen
import com.example.flowschannels.ui.lessons.hot.StateFlowScreen
import com.example.flowschannels.ui.lessons.hot.StateVsSharedScreen
import com.example.flowschannels.ui.lessons.hot.UiStatePatternScreen
import com.example.flowschannels.ui.lessons.operators.OperatorsOverviewScreen
import com.example.flowschannels.ui.lessons.operators.OperatorsPlaygroundScreen
import com.example.flowschannels.ui.lessons.search.SearchScreen
import com.example.flowschannels.ui.lessons.testing.TestingScreen

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(
        navController = navController,
        startDestination = Destinations.DASHBOARD,
        modifier = modifier,
    ) {
        composable(Destinations.DASHBOARD) {
            DashboardScreen(onOpenLesson = { route -> navController.navigate(route) })
        }
        composable(Destinations.FLOW_BASICS) { FlowBasicsScreen(back) }
        composable(Destinations.COLD_FLOW) { ColdFlowScreen(back) }
        composable(Destinations.FLOW_LIFECYCLE) { FlowLifecycleScreen(back) }
        composable(Destinations.OPERATORS) { OperatorsOverviewScreen(back) }
        composable(Destinations.OPERATORS_PLAYGROUND) { OperatorsPlaygroundScreen(back) }
        composable(Destinations.COMBINING) { CombiningOverviewScreen(back) }
        composable(Destinations.COMBINE) { CombineScreen(back) }
        composable(Destinations.ZIP) { ZipScreen(back) }
        composable(Destinations.MERGE) { MergeScreen(back) }
        composable(Destinations.FLATMAP) { FlatMapScreen(back) }
        composable(Destinations.ERROR_HANDLING) { ErrorHandlingScreen(back) }
        composable(Destinations.CANCELLATION) { CancellationScreen(back) }
        composable(Destinations.HOT_FLOWS) { HotFlowsScreen(back) }
        composable(Destinations.STATE_FLOW) { StateFlowScreen(back) }
        composable(Destinations.UI_STATE) { UiStatePatternScreen(back) }
        composable(Destinations.SHARED_FLOW) { SharedFlowScreen(back) }
        composable(Destinations.STATE_VS_SHARED) { StateVsSharedScreen(back) }
        composable(Destinations.CHANNELS) { ChannelsScreen(back) }
        composable(Destinations.CHANNEL_CAPACITY) { ChannelCapacityScreen(back) }
        composable(Destinations.FLOW_VS_CHANNEL) { FlowVsChannelScreen(back) }
        composable(Destinations.CHANNEL_VS_SHARED) { ChannelVsSharedFlowScreen(back) }
        composable(Destinations.RECEIVE_AS_FLOW) { ReceiveAsFlowScreen(back) }
        composable(Destinations.CALLBACK_FLOW) { CallbackFlowScreen(back) }
        composable(Destinations.CHANNEL_FLOW) { ChannelFlowScreen(back) }
        composable(Destinations.FLOW_CONTEXT) { FlowContextScreen(back) }
        composable(Destinations.BACKPRESSURE) { BackpressureScreen(back) }
        composable(Destinations.FLOW_VIEWMODEL) { ViewModelFlowScreen(back) }
        composable(Destinations.FLOW_REPOSITORY) { RepositoryScreen(back) }
        composable(Destinations.FLOW_ROOM) { RoomScreen(back) }
        composable(Destinations.FLOW_RETROFIT) { RetrofitScreen(back) }
        composable(Destinations.FLOW_COMPOSE) { ComposeFlowScreen(back) }
        composable(Destinations.SEARCH) { SearchScreen(back) }
        composable(Destinations.TESTING) { TestingScreen(back) }
    }
}
