package com.simplyawakeremake.data.user

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface UserRepository {
    val currentUser: Flow<User>

    suspend fun ensureLocalUserExists(): User

    suspend fun linkCurrentUserToRemote(
        userRemoteId: String,
        email: String?,
        displayName: String?
    )

    suspend fun requireCurrentUserId(): String = currentUser.first().id

    suspend fun hasExistingUser(): Boolean
}