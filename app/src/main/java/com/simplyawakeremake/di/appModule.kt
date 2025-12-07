package com.simplyawakeremake.di

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.simplyawakeremake.BuildConfig
import com.simplyawakeremake.data.common.DataSaver
import com.simplyawakeremake.data.download.AndroidDownloadService
import com.simplyawakeremake.data.download.DownloadService
import com.simplyawakeremake.data.download.FileStorage
import com.simplyawakeremake.data.download.track.LocalTrackFileStorage
import com.simplyawakeremake.data.download.track.TrackDownloadStore
import com.simplyawakeremake.data.download.track.TrackDownloader
import com.simplyawakeremake.data.download.track.TrackFileManager
import com.simplyawakeremake.data.history.TrackHistorySaver
import com.simplyawakeremake.data.history.UiTrackHistory
import com.simplyawakeremake.data.track.TrackUriProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module


val Context.userPrefsDataStore by preferencesDataStore(
    name = "user_prefs"
)

val appModule = module {

    single<SharedPreferences> {
        androidContext().getSharedPreferences(
            "private_shared_preferences",
            Context.MODE_PRIVATE
        )
    }

    single<DataStore<Preferences>> {
        androidContext().userPrefsDataStore
    }

    single<DataSaver<UiTrackHistory>>(named("track_history")) { TrackHistorySaver(get()) }
    single { TrackUriProvider(BuildConfig.baseServerUrl) }

    single<FileStorage>(named("tracks")) { LocalTrackFileStorage(androidContext()) }
    single<DownloadService> { AndroidDownloadService(androidContext()) }

    single { TrackDownloadStore(fileStorage = get(named("tracks"))) }

    single {
        TrackDownloader(
            downloadService = get(),
            downloadStore = get()
        )
    }

    single<TrackFileManager> {
        TrackFileManager(
            get<FileStorage>(named("tracks")),
            get<TrackUriProvider>()::trackUri,
            get<TrackDownloadStore>(),
            get<TrackDownloader>()
        )
    }
}