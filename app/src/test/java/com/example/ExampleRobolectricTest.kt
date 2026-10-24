package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.LinkSaathiDatabase
import com.example.data.LinkSaathiRepository
import com.example.data.PreferencesDataSource
import com.example.data.UserPreferencesState
import com.example.model.RiskLevel
import com.example.ui.LinkSaathiApp
import com.example.ui.LinkSaathiViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FakePreferencesDataSource : PreferencesDataSource {
    private val state = MutableStateFlow(UserPreferencesState(isDarkTheme = true, isHistoryEnabled = true))
    override val preferencesFlow: Flow<UserPreferencesState> = state

    override suspend fun setDarkTheme(isDark: Boolean) {
        state.update { it.copy(isDarkTheme = isDark) }
    }

    override suspend fun setHistoryEnabled(enabled: Boolean) {
        state.update { it.copy(isHistoryEnabled = enabled) }
    }

    override suspend fun toggleChecklistItem(stepId: String) {
        state.update { current ->
            val updated = if (stepId in current.completedChecklistIds) {
                current.completedChecklistIds - stepId
            } else {
                current.completedChecklistIds + stepId
            }
            current.copy(completedChecklistIds = updated)
        }
    }

    override suspend fun resetChecklist() {
        state.update { it.copy(completedChecklistIds = emptySet()) }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: LinkSaathiDatabase
    private lateinit var repository: LinkSaathiRepository
    private lateinit var viewModel: LinkSaathiViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LinkSaathiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LinkSaathiRepository(database.scanHistoryDao(), FakePreferencesDataSource())
        viewModel = LinkSaathiViewModel(repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LinkSaathi", appName)
    }

    @Test
    fun `save scan history, persist across reload, clear history, and disable history storage`() = runBlocking {
        // 1. Analyze an OTP phishing message and save to history
        viewModel.updateMessageInput("URGENT: Share your 6-digit OTP 554433 with 9876512345 to unblock account.")
        viewModel.analyzeCurrentMessage()
        assertEquals(RiskLevel.HIGH, viewModel.uiState.value.activeScanResult?.riskLevel)

        val resultToSave = viewModel.uiState.value.activeScanResult!!
        assertTrue(repository.saveScanToHistory(resultToSave))

        val savedItems = repository.scanHistoryFlow.first()
        assertEquals(1, savedItems.size)
        // Verify sensitive OTP and phone number are redacted in stored DB record
        assertFalse(savedItems.first().redactedPreview.contains("554433"))
        assertTrue(savedItems.first().redactedPreview.contains("[REDACTED-CODE]"))

        // 2. Simulate page/app refresh by reading from a fresh repository instance backed by the same DB
        val reloadedRepo = LinkSaathiRepository(database.scanHistoryDao(), FakePreferencesDataSource())
        val persistedItems = reloadedRepo.scanHistoryFlow.first()
        assertEquals(1, persistedItems.size)

        // 3. Disable history storage and purge existing records
        reloadedRepo.setHistoryEnabled(enabled = false, clearExistingWhenDisabled = true)
        val afterClear = reloadedRepo.scanHistoryFlow.first()
        assertTrue(afterClear.isEmpty())

        // 4. Verify saving while history is disabled does not store anything
        viewModel.updateUrlInput("http://192.168.1.1/login")
        viewModel.analyzeCurrentUrl()
        val urlResult = viewModel.uiState.value.activeScanResult!!
        assertFalse(reloadedRepo.saveScanToHistory(urlResult))
        assertTrue(reloadedRepo.scanHistoryFlow.first().isEmpty())
    }

    @Test
    fun `verify mobile and desktop layouts and end to end URL check flow`() {
        composeTestRule.setContent {
            // Render in compact mobile viewport first
            Box(modifier = Modifier.size(width = 400.dp, height = 850.dp)) {
                LinkSaathiApp(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // Bottom navigation bar should be displayed on mobile
        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_check_link_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Enter suspicious URL and submit
        composeTestRule.onNodeWithTag("url_input_field").performTextInput("http://192.168.1.50/sbi-kyc")
        composeTestRule.onNodeWithTag("analyze_url_submit_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Results screen shows HIGH risk badge
        composeTestRule.onNodeWithTag("scan_results_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("risk_badge_high").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w960dp-h800dp")
    fun `verify desktop wide layout renders NavigationRail`() {
        composeTestRule.setContent {
            LinkSaathiApp(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("desktop_navigation_rail").assertIsDisplayed()
    }
}
