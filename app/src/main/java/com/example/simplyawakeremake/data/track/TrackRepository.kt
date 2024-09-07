package com.example.simplyawakeremake.data.track

import com.example.simplyawakeremake.UiTrack
import com.example.simplyawakeremake.data.common.DataRepository
import com.example.simplyawakeremake.data.common.DataSaver

class TrackRepository(trackService: TrackService, saver: DataSaver<ApiTrack>) :
    DataRepository<UiTrack, ApiTrack, ApiTrack>(trackService, saver) {

    override val dtoToDbMapper: (ApiTrack) -> ApiTrack = { it -> it }
    override val dbToUiModelMapper: (ApiTrack) -> UiTrack = { it.toUiTrack() }

}