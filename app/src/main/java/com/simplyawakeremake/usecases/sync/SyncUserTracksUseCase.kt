package com.simplyawakeremake.usecases.sync

import com.simplyawakeremake.data.user.UserRepository
import com.simplyawakeremake.data.usertrack.remote.UserTrackRemoteDataSource
import com.simplyawakeremake.data.usertrack.sync.UserTrackSyncResult
import com.simplyawakeremake.data.usertrack.sync.UserTrackSyncService
import kotlinx.coroutines.flow.first

class UserTrackLoginSyncUseCase(
    private val userRepository: UserRepository,
    private val remote: UserTrackRemoteDataSource,
    private val syncService: UserTrackSyncService,
) {

    companion object {
        private const val TAG = "UserTrackLoginSync"
    }

    suspend operator fun invoke(): UserTrackSyncResult {
        val user = userRepository.currentUser.first()
        val firebaseUid = user.firebaseUid ?: return UserTrackSyncResult.Failure(
            IllegalStateException("Cannot sync: current user is a guest (no firebaseUid)")
        )

        val remoteTracks = remote.fetchAllForUser(firebaseUid)
        return syncService.syncDownFromRemoteToLocal(remoteTracks)
    }
}