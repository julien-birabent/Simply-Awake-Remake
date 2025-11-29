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

class LinkCurrentUserToFirebaseException(
    cause: Throwable
) : Exception("Failed to link user to Firebase / Firestore", cause)


class UserRepositoryImpl(
    private val userDao: UserDao,
    private val dataStore: DataStore<Preferences>,
    private val remote: UserRemoteDataSource
) : UserRepository {

    companion object {
        private val KEY_CURRENT_USER_ID = stringPreferencesKey("current_user_id")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val currentUser: Flow<User> =
        dataStore.data
            .map { prefs -> prefs[KEY_CURRENT_USER_ID] }
            .flatMapLatest { id ->
                flow {
                    val user = if (id != null) {
                        userDao.getById(id)
                    } else {
                        null
                    }

                    val ensuredUser = user ?: createGuestUserInternal()
                    emit(ensuredUser.toDomain())
                }
            }

    override suspend fun ensureLocalUserExists(): User = currentUser.first()

    private suspend fun createGuestUserInternal(): UserEntity {
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

        Log.i("UserRepositoryImpl", "Creating guest user: $user")

        userDao.insert(user)
        dataStore.edit { prefsEditable ->
            prefsEditable[KEY_CURRENT_USER_ID] = newId
        }

        return user
    }

    override suspend fun linkCurrentUserToRemote(
        userRemoteId: String,
        email: String?,
        displayName: String?,
    ) {
        Log.i("UserRepositoryImpl", "linkCurrentUserToFirebase: $userRemoteId")

        val baseUser = currentUser.first()

        val updated = baseUser.copy(
            firebaseUid = userRemoteId,
            email = email ?: baseUser.email,
            displayName = displayName ?: baseUser.displayName
        )

        try {
            remote.upsertUser(updated)
        } catch (e: Exception) {
            Log.w("UserRepositoryImpl", "Failed to sync user to Firestore", e)
            throw LinkCurrentUserToFirebaseException(e)
        }

        userDao.insert(updated.toEntity())
    }



    override suspend fun hasExistingUser(): Boolean {
        val prefs = dataStore.data.first()
        val existingId = prefs[KEY_CURRENT_USER_ID] ?: return false

        return userDao.getById(existingId) != null
    }

}
