package com.example.simplyawakeremake.data.track

import android.content.SharedPreferences
import com.example.simplyawakeremake.data.common.DataSaver
import com.example.simplyawakeremake.extensions.dataCollection
import io.reactivex.rxjava3.core.Single

class TrackSharedPrefsSaver(sharedPreferences: SharedPreferences) : DataSaver<ApiTrack> {

    private var tracks: List<ApiTrack> by sharedPreferences.dataCollection("saved_tracks")

    override fun persist(objects: List<ApiTrack>) {
        tracks = objects
    }

    override fun loadAll(): Single<List<ApiTrack>> {
        return Single.just(tracks)
    }

    override fun select(id: String): ApiTrack {
        return tracks.first { it.id == id }
    }
}