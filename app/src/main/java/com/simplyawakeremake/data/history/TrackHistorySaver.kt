package com.simplyawakeremake.data.history

import android.content.SharedPreferences
import com.simplyawakeremake.data.common.DataSaver
import com.simplyawakeremake.extensions.dataCollection

class TrackHistorySaver(sharedPreferences: SharedPreferences) : DataSaver<UiTrackHistory> {

    private var tracks: List<UiTrackHistory> by sharedPreferences.dataCollection<UiTrackHistory>("saved_track_history")

    override fun persist(objects: List<UiTrackHistory>) {
        tracks = objects
    }

    override suspend fun loadAll(): List<UiTrackHistory> {
        return tracks
    }

    override fun select(id: String): UiTrackHistory? {
        return tracks.firstOrNull { it.track.id == id }
    }
}