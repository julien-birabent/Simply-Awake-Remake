package com.simplyawakeremake.di

import com.simplyawakeremake.data.history.TrackHistoryRepository
import com.simplyawakeremake.data.history.TrackHistoryRepositoryInterface
import com.simplyawakeremake.data.track.repository.TrackRepository
import com.simplyawakeremake.data.track.repository.TrackRepositoryInterface
import com.simplyawakeremake.data.usertrack.TrackWithUserRepository
import com.simplyawakeremake.data.usertrack.UserTrackRepository
import org.koin.dsl.module

val repositoryModule = module {

    single { TrackRepository() }
    single {
        UserTrackRepository(
            userId = "1",
            local = get(),
        )
    }

    single<TrackHistoryRepositoryInterface> { TrackHistoryRepository() }

    single<TrackRepositoryInterface> {
        TrackWithUserRepository(
            catalogRepo = get<TrackRepository>(),
            userTrackRepository = get(),
        )
    }
}