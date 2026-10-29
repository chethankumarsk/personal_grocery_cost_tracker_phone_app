package dev.groceryledger.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.groceryledger.domain.model.AppSettings
import dev.groceryledger.domain.model.ThemeMode
import dev.groceryledger.domain.model.WeekStart
import dev.groceryledger.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository(
    private val context: Context,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences ->
            AppSettings(
                weekStart = preferences[Keys.WEEK_START]
                    ?.let { stored -> WeekStart.entries.firstOrNull { it.name == stored } }
                    ?: WeekStart.MONDAY,
                theme = preferences[Keys.THEME]
                    ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
                    ?: ThemeMode.SYSTEM,
            )
        }
        .distinctUntilChanged()

    override suspend fun setWeekStart(weekStart: WeekStart) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.WEEK_START] = weekStart.name
        }
    }

    override suspend fun setTheme(theme: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.THEME] = theme.name
        }
    }

    private object Keys {
        val WEEK_START = stringPreferencesKey("week_start")
        val THEME = stringPreferencesKey("theme")
    }
}
