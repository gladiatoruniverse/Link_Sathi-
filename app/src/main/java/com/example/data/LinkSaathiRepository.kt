package com.example.data

import com.example.model.ScanResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class LinkSaathiRepository(
    private val scanHistoryDao: ScanHistoryDao,
    private val preferencesDataSource: PreferencesDataSource
) {
    val scanHistoryFlow: Flow<List<ScanResult>> =
        scanHistoryDao.getAllHistory().map { entities ->
            entities.map { it.toDomainModel() }
        }

    val userPreferencesFlow: Flow<UserPreferencesState> =
        preferencesDataSource.preferencesFlow

    suspend fun saveScanToHistory(result: ScanResult): Boolean {
        val currentPrefs = preferencesDataSource.preferencesFlow.first()
        if (!currentPrefs.isHistoryEnabled) {
            return false
        }
        val entity = ScanHistoryEntity.fromDomainModel(result.copy(isSavedToHistory = true))
        scanHistoryDao.insertScan(entity)
        return true
    }

    suspend fun deleteHistoryItem(id: String) {
        scanHistoryDao.deleteScanById(id)
    }

    suspend fun clearAllHistory() {
        scanHistoryDao.clearAllHistory()
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        preferencesDataSource.setDarkTheme(isDark)
    }

    suspend fun setHistoryEnabled(enabled: Boolean, clearExistingWhenDisabled: Boolean = false) {
        preferencesDataSource.setHistoryEnabled(enabled)
        if (!enabled && clearExistingWhenDisabled) {
            scanHistoryDao.clearAllHistory()
        }
    }

    suspend fun toggleEmergencyChecklistStep(stepId: String) {
        preferencesDataSource.toggleChecklistItem(stepId)
    }

    suspend fun resetEmergencyChecklist() {
        preferencesDataSource.resetChecklist()
    }
}
