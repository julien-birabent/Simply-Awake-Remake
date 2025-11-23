package com.simplyawakeremake.data.usertrack.remote

import com.simplyawakeremake.data.usertrack.local.UserTrackEntity

interface UserTrackRemoteDataSource {

    /**
     * Push the given user track state to the remote backend.
     * DB is the source of truth; this is a mirror operation.
     */
    suspend fun upsertUserTrack(entity: UserTrackEntity)

    /**
     * Optional, for initial sync / restore.
     * Typically used when local DB is empty.
     */
    suspend fun fetchAllForUser(userId: String): List<UserTrackRemote>
}
