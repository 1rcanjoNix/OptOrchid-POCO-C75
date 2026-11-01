package com.optorchid.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.optorchid.domain.model.AppSettings
import com.optorchid.domain.repository.SettingsRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "orchid_settings")

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private val reduceMotionKey = booleanPreferencesKey("reduce_motion")

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs -> AppSettings(reduceMotion = prefs[reduceMotionKey] ?: false) }

    override suspend fun setReduceMotion(enabled: Boolean) {
        dataStore.edit { it[reduceMotionKey] = enabled }
    }
}
