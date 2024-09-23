package com.simplyawakeremake.data.track

import android.content.SharedPreferences
import com.simplyawakeremake.data.common.DataSaver
import com.simplyawakeremake.extensions.dataCollection
import io.reactivex.rxjava3.core.Single

class TrackSharedPrefsSaver(sharedPreferences: SharedPreferences) : DataSaver<ApiTrack> {

    private var tracks: List<ApiTrack> by sharedPreferences.dataCollection<ApiTrack>("saved_tracks")

    override fun persist(objects: List<ApiTrack>) {
        tracks = objects
    }

    override fun loadAll(): Single<List<ApiTrack>> {
        return Single.just(tracks)
    }

    override fun select(id: String): ApiTrack? {
        return tracks.firstOrNull { it.id == id }
    }
}