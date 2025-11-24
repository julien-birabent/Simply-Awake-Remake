package com.simplyawakeremake.data.user

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.simplyawakeremake.now
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.UUID

class UserRepositoryImpl(
    private val userDao: UserDao,
    private val dataStore: DataStore<Preferences>,
) : UserRepository {

    companion object {
        private val KEY_CURRENT_USER_ID = stringPreferencesKey("current_user_id")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val currentUser: Flow<User> =
        dataStore.data
            .map { prefs -> prefs[KEY_CURRENT_USER_ID] }
            .filterNotNull()
            .flatMapLatest { id ->
                flow {
                    val user = userDao.getById(id) ?: error("No user for id=$id")
                    emit(user.toDomain())
                }
            }

    override suspend fun ensureGuestUser(): User {
        val prefs = dataStore.data.first()
        val existingId = prefs[KEY_CURRENT_USER_ID]

        if (existingId != null) {
            val existingUser = userDao.getById(existingId)
            if (existingUser != null) {
                Log.i("UserRepositoryImpl", "guest user already registered, returning it: $existingUser")
                return existingUser.toDomain()
            }
        }

        val newId = "user_" + UUID.randomUUID().toString()
        val now = now()

        val user = UserEntity(
            id = newId,
            firebaseUid = null,
            email = null,
            displayName = null,
            createdAt = now,
            lastActiveAt = now,
        )
        Log.i("UserRepositoryImpl", "No Guest User currently registered; creating new user: $user")

        userDao.insert(user)

        dataStore.edit { prefsEditable ->
            prefsEditable[KEY_CURRENT_USER_ID] = newId
        }

        return user.toDomain()
    }

    override suspend fun linkCurrentUserToFirebase(
        firebaseUid: String,
        email: String?,
        displayName: String?,
    ) {
        Log.i("UserRepositoryImpl", "linkCurrentUserToFirebase: $firebaseUid")
        val current = currentUser.first()
        val updated = current.copy(
            firebaseUid = firebaseUid,
            email = email ?: current.email,
            displayName = displayName ?: current.displayName
        )
        userDao.insert(updated.toEntity())
    }
}
