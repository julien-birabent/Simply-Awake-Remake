package com.simplyawakeremake.data.usertrack.remote

import com.simplyawakeremake.data.usertrack.UserTrack

/**
 * Remote data source for user-track metadata.
 *
 * It only knows about the domain model [UserTrack], not the local DB entity,
 * so the remote layer stays decoupled from Room.
 */
interface UserTrackRemoteDataSource {

    /**
     * Push the given user track state to the remote backend.
     * Local DB is the source of truth; this is a mirror operation.
     */
    suspend fun upsertUserTrack(remoteUserId: String, userTrack: UserTrack)

    /**
     * Optional, for initial sync / restore.
     * Typically used when local DB is empty.
     */
    suspend fun fetchAllForUser(remoteUserId: String): List<UserTrack>
}
