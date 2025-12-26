package com.simplyawakeremake.data.playback

import com.simplyawakeremake.data.user.userPreferencesDataStore


import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class PlaybackPreferencesRepositoryImpl(context: Context) : PlaybackPreferencesRepository {

    private val dataStore = context.userPreferencesDataStore

    private val immersiveModeKey = booleanPreferencesKey("immersive_mode_enabled")

    override val immersiveModeEnabled: Flow<Boolean> =
        dataStore.data
            .map { prefs -> prefs[immersiveModeKey] ?: true }
            .distinctUntilChanged()

    override suspend fun setImmersiveModeEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[immersiveModeKey] = enabled
        }
    }
}
