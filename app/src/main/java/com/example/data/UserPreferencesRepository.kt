package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.linkSaathiDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "linksaathi_preferences"
)

data class UserPreferencesState(
    val isDarkTheme: Boolean = true,
    val isHistoryEnabled: Boolean = true,
    val completedChecklistIds: Set<String> = emptySet()
)

interface PreferencesDataSource {
    val preferencesFlow: Flow<UserPreferencesState>
    suspend fun setDarkTheme(isDark: Boolean)
    suspend fun setHistoryEnabled(enabled: Boolean)
    suspend fun toggleChecklistItem(stepId: String)
    suspend fun resetChecklist()
}

class DataStorePreferencesDataSource(private val context: Context) : PreferencesDataSource {

    companion object {
        private val KEY_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        private val KEY_HISTORY_ENABLED = booleanPreferencesKey("is_history_enabled")
        private val KEY_COMPLETED_CHECKLIST = stringSetPreferencesKey("completed_checklist_ids")
    }

    override val preferencesFlow: Flow<UserPreferencesState> =
        context.linkSaathiDataStore.data.map { prefs ->
            UserPreferencesState(
                isDarkTheme = prefs[KEY_DARK_THEME] ?: true, // Default Dark Navy theme per user preference
                isHistoryEnabled = prefs[KEY_HISTORY_ENABLED] ?: true,
                completedChecklistIds = prefs[KEY_COMPLETED_CHECKLIST] ?: emptySet()
            )
        }

    override suspend fun setDarkTheme(isDark: Boolean) {
        context.linkSaathiDataStore.edit { prefs ->
            prefs[KEY_DARK_THEME] = isDark
        }
    }

    override suspend fun setHistoryEnabled(enabled: Boolean) {
        context.linkSaathiDataStore.edit { prefs ->
            prefs[KEY_HISTORY_ENABLED] = enabled
        }
    }

    override suspend fun toggleChecklistItem(stepId: String) {
        context.linkSaathiDataStore.edit { prefs ->
            val current = prefs[KEY_COMPLETED_CHECKLIST] ?: emptySet()
            prefs[KEY_COMPLETED_CHECKLIST] = if (stepId in current) {
                current - stepId
            } else {
                current + stepId
            }
        }
    }

    override suspend fun resetChecklist() {
        context.linkSaathiDataStore.edit { prefs ->
            prefs[KEY_COMPLETED_CHECKLIST] = emptySet()
        }
    }
}
