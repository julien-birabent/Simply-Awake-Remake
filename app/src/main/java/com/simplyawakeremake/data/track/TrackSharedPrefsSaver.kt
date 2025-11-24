package com.simplyawakeremake.data.track

import android.content.SharedPreferences
import com.simplyawakeremake.data.common.DataSaver
import com.simplyawakeremake.data.track.remote.TrackDto
import com.simplyawakeremake.extensions.dataCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrackSharedPrefsSaver(sharedPreferences: SharedPreferences) : DataSaver<TrackDto> {

    private var tracks: List<TrackDto> by sharedPreferences.dataCollection<TrackDto>("saved_tracks")

    override fun persist(objects: List<TrackDto>) {
        tracks = objects
    }

    override suspend fun loadAll(): List<TrackDto> = withContext(Dispatchers.IO) { tracks }

    override fun select(id: String): TrackDto? {
        return tracks.firstOrNull { it.id == id }
    }
}