package com.simplyawakeremake.data.usertrack.sync

import com.simplyawakeremake.data.usertrack.UserTrack

interface UserTrackSyncService {
    suspend fun syncUpFromLocalToRemote(localTracks: List<UserTrack>): UserTrackSyncResult
    suspend fun syncDownFromRemoteToLocal(remoteTracks: List<UserTrack>): UserTrackSyncResult
}
