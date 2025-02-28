package com.simplyawakeremake.data.track

import android.content.SharedPreferences
import com.simplyawakeremake.data.common.DataSaver
import com.simplyawakeremake.extensions.dataCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrackSharedPrefsSaver(sharedPreferences: SharedPreferences) : DataSaver<ApiTrack> {

    private var tracks: List<ApiTrack> by sharedPreferences.dataCollection<ApiTrack>("saved_tracks")

    override fun persist(objects: List<ApiTrack>) {
        tracks = objects
    }

    override suspend fun loadAll(): List<ApiTrack> = withContext(Dispatchers.IO) { tracks }

    override fun select(id: String): ApiTrack? {
        return tracks.firstOrNull { it.id == id }
    }
}