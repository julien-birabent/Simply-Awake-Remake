package com.simplyawakeremake.data.user

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val USER_PREFERENCES_NAME = "user_preferences"

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = USER_PREFERENCES_NAME
)
