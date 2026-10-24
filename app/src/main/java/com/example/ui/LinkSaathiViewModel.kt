package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DataStorePreferencesDataSource
import com.example.data.LinkSaathiDatabase
import com.example.data.LinkSaathiRepository
import com.example.data.UserPreferencesState
import com.example.domain.MessageScamAnalyzer
import com.example.domain.UrlHeuristicAnalyzer
import com.example.model.AppScreen
import com.example.model.LinkSaathiPresets
import com.example.model.RiskLevel
import com.example.model.SampleInputPreset
import com.example.model.ScanResult
import com.example.model.ScanType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LinkSaathiUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val navigationBackStack: List<AppScreen> = emptyList(),
    val urlInput: String = "",
    val urlError: String? = null,
    val messageInput: String = "",
    val messageError: String? = null,
    val activeScanResult: ScanResult? = null,
    val historyTypeFilter: ScanType? = null,
    val historyRiskFilter: RiskLevel? = null,
    val currentSafetyTipIndex: Int = 0,
    val bannerFeedbackMessage: String? = null
)

class LinkSaathiViewModel(
    private val repository: LinkSaathiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LinkSaathiUiState())
    val uiState: StateFlow<LinkSaathiUiState> = _uiState.asStateFlow()

    val scanHistory: StateFlow<List<ScanResult>> = repository.scanHistoryFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userPreferences: StateFlow<UserPreferencesState> = repository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferencesState()
        )

    fun navigateTo(destination: AppScreen) {
        _uiState.update { state ->
            if (state.currentScreen == destination) {
                state
            } else {
                val updatedStack = (state.navigationBackStack + state.currentScreen).takeLast(10)
                state.copy(
                    currentScreen = destination,
                    navigationBackStack = updatedStack,
                    urlError = null,
                    messageError = null,
                    bannerFeedbackMessage = null
                )
            }
        }
    }

    fun navigateBack(): Boolean {
        val currentState = _uiState.value
        if (currentState.currentScreen == AppScreen.HOME) {
            return false
        }
        val previous = currentState.navigationBackStack.lastOrNull() ?: AppScreen.HOME
        val remainingStack = if (currentState.navigationBackStack.isNotEmpty()) {
            currentState.navigationBackStack.dropLast(1)
        } else {
            emptyList()
        }
        _uiState.update {
            it.copy(
                currentScreen = previous,
                navigationBackStack = remainingStack,
                bannerFeedbackMessage = null
            )
        }
        return true
    }

    fun updateUrlInput(value: String) {
        _uiState.update {
            it.copy(urlInput = value, urlError = null, bannerFeedbackMessage = null)
        }
    }

    fun clearUrlInput() {
        _uiState.update {
            it.copy(urlInput = "", urlError = null)
        }
    }

    fun applyUrlSample(preset: SampleInputPreset) {
        _uiState.update {
            it.copy(urlInput = preset.content, urlError = null)
        }
    }

    fun analyzeCurrentUrl() {
        val raw = _uiState.value.urlInput
        when (val outcome = UrlHeuristicAnalyzer.analyze(raw)) {
            is UrlHeuristicAnalyzer.ValidationOutcome.InvalidEmpty -> {
                _uiState.update { it.copy(urlError = outcome.message) }
            }
            is UrlHeuristicAnalyzer.ValidationOutcome.Ready -> {
                _uiState.update { state ->
                    state.copy(
                        urlError = null,
                        activeScanResult = outcome.result,
                        currentScreen = AppScreen.SCAN_RESULTS,
                        navigationBackStack = (state.navigationBackStack + state.currentScreen).takeLast(10),
                        bannerFeedbackMessage = null
                    )
                }
            }
        }
    }

    fun updateMessageInput(value: String) {
        _uiState.update {
            it.copy(messageInput = value, messageError = null, bannerFeedbackMessage = null)
        }
    }

    fun clearMessageInput() {
        _uiState.update {
            it.copy(messageInput = "", messageError = null)
        }
    }

    fun applyMessageSample(preset: SampleInputPreset) {
        _uiState.update {
            it.copy(messageInput = preset.content, messageError = null)
        }
    }

    fun analyzeCurrentMessage() {
        val raw = _uiState.value.messageInput
        when (val outcome = MessageScamAnalyzer.analyze(raw)) {
            is MessageScamAnalyzer.ValidationOutcome.InvalidEmpty -> {
                _uiState.update { it.copy(messageError = outcome.message) }
            }
            is MessageScamAnalyzer.ValidationOutcome.Ready -> {
                _uiState.update { state ->
                    state.copy(
                        messageError = null,
                        activeScanResult = outcome.result,
                        currentScreen = AppScreen.SCAN_RESULTS,
                        navigationBackStack = (state.navigationBackStack + state.currentScreen).takeLast(10),
                        bannerFeedbackMessage = null
                    )
                }
            }
        }
    }

    fun saveActiveResultToHistory() {
        val currentResult = _uiState.value.activeScanResult ?: return
        if (currentResult.isSavedToHistory) return

        viewModelScope.launch {
            val saved = repository.saveScanToHistory(currentResult)
            if (saved) {
                _uiState.update { state ->
                    state.copy(
                        activeScanResult = currentResult.copy(isSavedToHistory = true),
                        bannerFeedbackMessage = "Saved to local history with sensitive numbers & codes redacted."
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        bannerFeedbackMessage = "Scan History is currently disabled in Settings."
                    )
                }
            }
        }
    }

    fun checkAnotherFromResults() {
        val currentResult = _uiState.value.activeScanResult
        val targetScreen = if (currentResult?.scanType == ScanType.MESSAGE) {
            AppScreen.MESSAGE_ANALYZER
        } else {
            AppScreen.URL_CHECKER
        }
        _uiState.update { state ->
            state.copy(
                currentScreen = targetScreen,
                urlInput = if (targetScreen == AppScreen.URL_CHECKER) "" else state.urlInput,
                messageInput = if (targetScreen == AppScreen.MESSAGE_ANALYZER) "" else state.messageInput,
                urlError = null,
                messageError = null,
                bannerFeedbackMessage = null
            )
        }
    }

    fun inspectHistoryRecord(record: ScanResult) {
        _uiState.update { state ->
            state.copy(
                activeScanResult = record.copy(isSavedToHistory = true),
                currentScreen = AppScreen.SCAN_RESULTS,
                navigationBackStack = (state.navigationBackStack + state.currentScreen).takeLast(10),
                bannerFeedbackMessage = null
            )
        }
    }

    fun deleteHistoryRecord(id: String) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
            _uiState.update {
                it.copy(bannerFeedbackMessage = "Scan record removed from local storage.")
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            _uiState.update { state ->
                state.copy(
                    activeScanResult = state.activeScanResult?.copy(isSavedToHistory = false),
                    bannerFeedbackMessage = "All local scan history has been permanently cleared."
                )
            }
        }
    }

    fun setHistoryTypeFilter(scanType: ScanType?) {
        _uiState.update { it.copy(historyTypeFilter = scanType) }
    }

    fun setHistoryRiskFilter(riskLevel: RiskLevel?) {
        _uiState.update { it.copy(historyRiskFilter = riskLevel) }
    }

    fun nextSafetyTip() {
        val totalTips = LinkSaathiPresets.safetyTips.size
        if (totalTips > 0) {
            _uiState.update { state ->
                state.copy(currentSafetyTipIndex = (state.currentSafetyTipIndex + 1) % totalTips)
            }
        }
    }

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            repository.setDarkTheme(isDark)
        }
    }

    fun setHistoryEnabled(enabled: Boolean, clearExisting: Boolean = false) {
        viewModelScope.launch {
            repository.setHistoryEnabled(enabled, clearExistingWhenDisabled = clearExisting)
            _uiState.update {
                it.copy(
                    bannerFeedbackMessage = if (enabled) {
                        "Local scan history enabled."
                    } else if (clearExisting) {
                        "Scan history disabled and existing records purged."
                    } else {
                        "Scan history disabled for future scans."
                    }
                )
            }
        }
    }

    fun toggleEmergencyChecklistStep(stepId: String) {
        viewModelScope.launch {
            repository.toggleEmergencyChecklistStep(stepId)
        }
    }

    fun resetEmergencyChecklist() {
        viewModelScope.launch {
            repository.resetEmergencyChecklist()
        }
    }

    fun dismissBannerFeedback() {
        _uiState.update { it.copy(bannerFeedbackMessage = null) }
    }

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = LinkSaathiDatabase.getInstance(appContext)
            val prefs = DataStorePreferencesDataSource(appContext)
            val repo = LinkSaathiRepository(db.scanHistoryDao(), prefs)
            return LinkSaathiViewModel(repo) as T
        }
    }
}
