package com.simplyawakeremake.di

import com.google.firebase.firestore.FirebaseFirestore
import com.simplyawakeremake.data.history.TrackHistoryRepository
import com.simplyawakeremake.data.history.TrackHistoryRepositoryInterface
import com.simplyawakeremake.data.track.repository.TrackRepository
import com.simplyawakeremake.data.track.repository.TrackRepositoryInterface
import com.simplyawakeremake.data.usertrack.TrackWithUserRepository
import com.simplyawakeremake.data.usertrack.UserTrackRepository
import com.simplyawakeremake.data.usertrack.remote.FirestoreUserTrackRemoteDataSource
import com.simplyawakeremake.data.usertrack.remote.UserTrackRemoteDataSource
import org.koin.dsl.module

val repositoryModule = module {

    single { TrackRepository() }
    single { FirebaseFirestore.getInstance() }

    single<UserTrackRemoteDataSource> {
        FirestoreUserTrackRemoteDataSource(
            firestore = get(),
        )
    }

    single {
        UserTrackRepository(
            userId = "1",
            local = get(),
            remote = get(),
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