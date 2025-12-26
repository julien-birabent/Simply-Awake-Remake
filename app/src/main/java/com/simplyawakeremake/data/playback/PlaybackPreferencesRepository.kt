package com.simplyawakeremake.data.playback

import kotlinx.coroutines.flow.Flow

interface PlaybackPreferencesRepository {
    val immersiveModeEnabled: Flow<Boolean>
    suspend fun setImmersiveModeEnabled(enabled: Boolean)
}