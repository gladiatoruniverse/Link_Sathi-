package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppScreen
import com.example.model.LinkSaathiPresets
import com.example.ui.components.FeedbackToastBanner
import com.example.ui.components.LinkSaathiShieldLogo
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.MessageAnalyzerScreen
import com.example.ui.screens.SafetyCenterScreen
import com.example.ui.screens.ScanHistoryScreen
import com.example.ui.screens.ScanResultsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UrlCheckerScreen
import com.example.ui.theme.LinkSaathiTheme

private data class NavDestinationItem(
    val screen: AppScreen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val PRIMARY_NAV_ITEMS = listOf(
    NavDestinationItem(
        screen = AppScreen.HOME,
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_item_home"
    ),
    NavDestinationItem(
        screen = AppScreen.URL_CHECKER,
        label = "Check Link",
        selectedIcon = Icons.Filled.Link,
        unselectedIcon = Icons.Outlined.Link,
        testTag = "nav_item_url_checker"
    ),
    NavDestinationItem(
        screen = AppScreen.MESSAGE_ANALYZER,
        label = "Analyze Text",
        selectedIcon = Icons.Filled.Sms,
        unselectedIcon = Icons.Outlined.Sms,
        testTag = "nav_item_message_analyzer"
    ),
    NavDestinationItem(
        screen = AppScreen.HISTORY,
        label = "History",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_item_history"
    ),
    NavDestinationItem(
        screen = AppScreen.SAFETY_CENTER,
        label = "Safety",
        selectedIcon = Icons.Filled.Security,
        unselectedIcon = Icons.Outlined.Security,
        testTag = "nav_item_safety"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkSaathiApp(
    viewModel: LinkSaathiViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scanHistory by viewModel.scanHistory.collectAsStateWithLifecycle()
    val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()

    val currentTip = LinkSaathiPresets.safetyTips.getOrElse(uiState.currentSafetyTipIndex) {
        LinkSaathiPresets.safetyTips.first()
    }

    LinkSaathiTheme(darkTheme = userPrefs.isDarkTheme) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideLayout = maxWidth >= 700.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.currentScreen != AppScreen.HOME) {
                                    IconButton(
                                        onClick = { viewModel.navigateBack() },
                                        modifier = Modifier.testTag("top_bar_back_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Navigate back"
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                LinkSaathiShieldLogo(
                                    size = 38.dp,
                                    contentDescription = "LinkSaathi official header logo"
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "LinkSaathi",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Your Personal Digital Safety Assistant",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { viewModel.setDarkTheme(!userPrefs.isDarkTheme) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("top_bar_theme_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (userPrefs.isDarkTheme) {
                                        Icons.Outlined.LightMode
                                    } else {
                                        Icons.Outlined.DarkMode
                                    },
                                    contentDescription = if (userPrefs.isDarkTheme) {
                                        "Switch to Light Theme"
                                    } else {
                                        "Switch to Dark Theme"
                                    },
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("top_bar_settings_button")
                            ) {
                                Icon(
                                    imageVector = if (uiState.currentScreen == AppScreen.SETTINGS) {
                                        Icons.Filled.Settings
                                    } else {
                                        Icons.Outlined.Settings
                                    },
                                    contentDescription = "Open Settings",
                                    tint = if (uiState.currentScreen == AppScreen.SETTINGS) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    if (!isWideLayout) {
                        Column {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.testTag("bottom_navigation_bar")
                            ) {
                                PRIMARY_NAV_ITEMS.forEach { item ->
                                    val isSelected = uiState.currentScreen == item.screen ||
                                        (uiState.currentScreen == AppScreen.SCAN_RESULTS &&
                                            ((uiState.activeScanResult?.scanType?.name == "URL" && item.screen == AppScreen.URL_CHECKER) ||
                                                (uiState.activeScanResult?.scanType?.name == "MESSAGE" && item.screen == AppScreen.MESSAGE_ANALYZER)))

                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { viewModel.navigateTo(item.screen) },
                                        icon = {
                                            if (item.screen == AppScreen.HISTORY && scanHistory.isNotEmpty()) {
                                                BadgedBox(
                                                    badge = {
                                                        Badge { Text("${scanHistory.size}") }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                        contentDescription = item.label
                                                    )
                                                }
                                            } else {
                                                Icon(
                                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                    contentDescription = item.label
                                                )
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = item.label,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag(item.testTag)
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWideLayout) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxHeight()
                                .testTag("desktop_navigation_rail")
                        ) {
                            PRIMARY_NAV_ITEMS.forEach { item ->
                                val isSelected = uiState.currentScreen == item.screen
                                NavigationRailItem(
                                    selected = isSelected,
                                    onClick = { viewModel.navigateTo(item.screen) },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) },
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .testTag("${item.testTag}_rail")
                                )
                            }
                        }
                        VerticalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        val bannerMsg = uiState.bannerFeedbackMessage
                        if (bannerMsg != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                FeedbackToastBanner(
                                    message = bannerMsg,
                                    onDismiss = { viewModel.dismissBannerFeedback() }
                                )
                            }
                        }

                        Crossfade(
                            targetState = uiState.currentScreen,
                            label = "screen_crossfade",
                            modifier = Modifier.weight(1f)
                        ) { screen ->
                            when (screen) {
                                AppScreen.HOME -> HomeDashboardScreen(
                                    recentScans = scanHistory,
                                    currentTip = currentTip,
                                    isHistoryEnabled = userPrefs.isHistoryEnabled,
                                    onCheckLinkClick = { viewModel.navigateTo(AppScreen.URL_CHECKER) },
                                    onAnalyzeMessageClick = { viewModel.navigateTo(AppScreen.MESSAGE_ANALYZER) },
                                    onNextTipClick = { viewModel.nextSafetyTip() },
                                    onOpenSafetyCenterClick = { viewModel.navigateTo(AppScreen.SAFETY_CENTER) },
                                    onOpenHistoryClick = { viewModel.navigateTo(AppScreen.HISTORY) },
                                    onInspectHistoryItem = { viewModel.inspectHistoryRecord(it) }
                                )

                                AppScreen.URL_CHECKER -> UrlCheckerScreen(
                                    urlInput = uiState.urlInput,
                                    urlError = uiState.urlError,
                                    onUrlInputChange = { viewModel.updateUrlInput(it) },
                                    onClearInput = { viewModel.clearUrlInput() },
                                    onSelectSample = { viewModel.applyUrlSample(it) },
                                    onAnalyzeUrl = { viewModel.analyzeCurrentUrl() },
                                    onNavigateBack = { viewModel.navigateBack() }
                                )

                                AppScreen.MESSAGE_ANALYZER -> MessageAnalyzerScreen(
                                    messageInput = uiState.messageInput,
                                    messageError = uiState.messageError,
                                    onMessageInputChange = { viewModel.updateMessageInput(it) },
                                    onClearInput = { viewModel.clearMessageInput() },
                                    onSelectSample = { viewModel.applyMessageSample(it) },
                                    onAnalyzeMessage = { viewModel.analyzeCurrentMessage() },
                                    onNavigateBack = { viewModel.navigateBack() }
                                )

                                AppScreen.SCAN_RESULTS -> ScanResultsScreen(
                                    result = uiState.activeScanResult,
                                    isHistoryEnabled = userPrefs.isHistoryEnabled,
                                    onSaveToHistory = { viewModel.saveActiveResultToHistory() },
                                    onCheckAnother = { viewModel.checkAnotherFromResults() },
                                    onNavigateHome = { viewModel.navigateTo(AppScreen.HOME) },
                                    onNavigateBack = { viewModel.navigateBack() }
                                )

                                AppScreen.HISTORY -> ScanHistoryScreen(
                                    historyList = scanHistory,
                                    typeFilter = uiState.historyTypeFilter,
                                    riskFilter = uiState.historyRiskFilter,
                                    isHistoryEnabled = userPrefs.isHistoryEnabled,
                                    onSelectTypeFilter = { viewModel.setHistoryTypeFilter(it) },
                                    onSelectRiskFilter = { viewModel.setHistoryRiskFilter(it) },
                                    onInspectRecord = { viewModel.inspectHistoryRecord(it) },
                                    onDeleteRecord = { viewModel.deleteHistoryRecord(it) },
                                    onClearAllHistory = { viewModel.clearAllHistory() },
                                    onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) },
                                    onNavigateBack = { viewModel.navigateBack() }
                                )

                                AppScreen.SAFETY_CENTER -> SafetyCenterScreen(
                                    completedChecklistIds = userPrefs.completedChecklistIds,
                                    onToggleChecklistStep = { viewModel.toggleEmergencyChecklistStep(it) },
                                    onResetChecklist = { viewModel.resetEmergencyChecklist() },
                                    onNavigateBack = { viewModel.navigateBack() }
                                )

                                AppScreen.SETTINGS -> SettingsScreen(
                                    isDarkTheme = userPrefs.isDarkTheme,
                                    isHistoryEnabled = userPrefs.isHistoryEnabled,
                                    savedHistoryCount = scanHistory.size,
                                    onToggleDarkTheme = { viewModel.setDarkTheme(it) },
                                    onToggleHistoryEnabled = { enabled, clear ->
                                        viewModel.setHistoryEnabled(enabled, clear)
                                    },
                                    onClearAllHistory = { viewModel.clearAllHistory() },
                                    onNavigateBack = { viewModel.navigateBack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
