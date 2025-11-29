package com.simplyawakeremake.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.simplyawakeremake.data.auth.AuthRepository
import com.simplyawakeremake.data.auth.AuthRepositoryImpl
import com.simplyawakeremake.data.auth.createGoogleSignInClient
import com.simplyawakeremake.data.history.TrackHistoryRepository
import com.simplyawakeremake.data.history.TrackHistoryRepositoryInterface
import com.simplyawakeremake.data.track.repository.TrackRepository
import com.simplyawakeremake.data.track.repository.TrackRepositoryInterface
import com.simplyawakeremake.data.user.FirestoreUserRemoteDataSource
import com.simplyawakeremake.data.user.UserRemoteDataSource
import com.simplyawakeremake.data.user.UserRepository
import com.simplyawakeremake.data.user.UserRepositoryImpl
import com.simplyawakeremake.data.usertrack.TrackWithUserRepository
import com.simplyawakeremake.data.usertrack.UserTrackRepository
import com.simplyawakeremake.data.usertrack.remote.FirestoreUserTrackRemoteDataSource
import com.simplyawakeremake.data.usertrack.remote.UserTrackRemoteDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {

    single { TrackRepository() }
    single { FirebaseFirestore.getInstance() }
    single<FirebaseAuth> { FirebaseAuth.getInstance() }

    single {
        androidContext().createGoogleSignInClient()
    }

    single<UserTrackRemoteDataSource> {
        FirestoreUserTrackRemoteDataSource(
            firestore = get(),
        )
    }

    single<AuthRepository> {
        AuthRepositoryImpl(
            firebaseAuth = get(),
            userRepository = get()
        )
    }

    single {
        UserTrackRepository(
            local = get(),
            remote = get(),
            userRepository = get()
        )
    }

    single<TrackHistoryRepositoryInterface> { TrackHistoryRepository() }

    single<TrackRepositoryInterface> {
        TrackWithUserRepository(
            catalogRepo = get<TrackRepository>(),
            userTrackRepository = get(),
        )
    }

    single<UserRemoteDataSource> { FirestoreUserRemoteDataSource(get()) }
    single<UserRepository> {
        UserRepositoryImpl(get(), get(), get())
    }
}