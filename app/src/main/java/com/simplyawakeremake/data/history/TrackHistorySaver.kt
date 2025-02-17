package com.simplyawakeremake.data.history

import android.content.SharedPreferences
import com.simplyawakeremake.data.common.DataSaver
import com.simplyawakeremake.extensions.dataCollection
import io.reactivex.rxjava3.core.Single

class TrackHistorySaver(sharedPreferences: SharedPreferences) : DataSaver<UiTrackHistory> {

    private var tracks: List<UiTrackHistory> by sharedPreferences.dataCollection<UiTrackHistory>("saved_track_history")

    override fun persist(objects: List<UiTrackHistory>) {
        tracks = objects
    }

    override fun loadAll(): Single<List<UiTrackHistory>> {
        return Single.just(tracks)
    }

    override fun select(id: String): UiTrackHistory? {
        return tracks.firstOrNull { it.track.id == id }
    }
}