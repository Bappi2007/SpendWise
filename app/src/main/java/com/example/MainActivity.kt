package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AnalysisScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SpendWiseTheme
import com.example.ui.viewmodel.AnalysisViewModel
import com.example.ui.viewmodel.AnalysisViewModelFactory
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.ExpenseViewModelFactory
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.SettingsViewModelFactory
import com.example.ui.viewmodel.UiEvent
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

enum class AppDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
    ANALYSIS("Analysis", Icons.Filled.Insights, Icons.Outlined.Insights, "nav_tab_analysis"),
    CATEGORIES("Categories", Icons.Filled.Category, Icons.Outlined.Category, "nav_tab_categories"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_tab_settings")
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as SpendWiseApplication).container

        val expenseViewModel: ExpenseViewModel by viewModels {
            ExpenseViewModelFactory(appContainer.expenseRepository)
        }

        val analysisViewModel: AnalysisViewModel by viewModels {
            AnalysisViewModelFactory(appContainer.expenseRepository)
        }

        val settingsViewModel: SettingsViewModel by viewModels {
            SettingsViewModelFactory(
                preferencesRepository = appContainer.preferencesRepository,
                backupRestoreManager = appContainer.backupRestoreManager,
                reportExporter = appContainer.reportExporter,
                expenseRepository = appContainer.expenseRepository
            )
        }

        setContent {
            val userPrefs by expenseViewModel.userPreferences.collectAsStateWithLifecycle()

            val isDarkTheme = when (userPrefs.themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            SpendWiseTheme(darkTheme = isDarkTheme) {
                SpendWiseApp(
                    expenseViewModel = expenseViewModel,
                    analysisViewModel = analysisViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

@Composable
fun SpendWiseApp(
    expenseViewModel: ExpenseViewModel,
    analysisViewModel: AnalysisViewModel,
    settingsViewModel: SettingsViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }

    val userPreferences by expenseViewModel.userPreferences.collectAsStateWithLifecycle()
    val homeSummary by expenseViewModel.homeSummary.collectAsStateWithLifecycle()
    val groupedExpenses by expenseViewModel.groupedExpenses.collectAsStateWithLifecycle()
    val categories by expenseViewModel.categories.collectAsStateWithLifecycle()
    val monthSummary by analysisViewModel.monthSummary.collectAsStateWithLifecycle()
    val pendingRestore by settingsViewModel.pendingRestore.collectAsStateWithLifecycle()
    val isProcessing by settingsViewModel.isProcessing.collectAsStateWithLifecycle()

    // Listen to ViewModel events (Snackbars, undo actions)
    LaunchedEffect(Unit) {
        merge(
            expenseViewModel.eventFlow,
            settingsViewModel.eventFlow
        ).collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        duration = if (event.actionLabel != null) SnackbarDuration.Short else SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        event.onAction?.invoke()
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                AppDestination.entries.forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.title
                            )
                        },
                        label = { Text(destination.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag(destination.testTag)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Crossfade(
            targetState = currentDestination,
            label = "screen_crossfade",
            modifier = Modifier.padding(innerPadding)
        ) { destination ->
            when (destination) {
                AppDestination.HOME -> {
                    HomeScreen(
                        summary = homeSummary,
                        groupedExpenses = groupedExpenses,
                        categories = categories,
                        currencySymbol = userPreferences.currencySymbol,
                        viewModel = expenseViewModel
                    )
                }
                AppDestination.ANALYSIS -> {
                    AnalysisScreen(
                        summary = monthSummary,
                        categories = categories,
                        currencySymbol = userPreferences.currencySymbol,
                        viewModel = analysisViewModel
                    )
                }
                AppDestination.CATEGORIES -> {
                    CategoriesScreen(
                        categories = categories,
                        viewModel = expenseViewModel
                    )
                }
                AppDestination.SETTINGS -> {
                    SettingsScreen(
                        preferences = userPreferences,
                        categories = categories,
                        pendingRestore = pendingRestore,
                        isProcessing = isProcessing,
                        viewModel = settingsViewModel
                    )
                }
            }
        }
    }
}
