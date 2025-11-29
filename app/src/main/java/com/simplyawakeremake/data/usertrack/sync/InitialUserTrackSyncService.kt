package com.simplyawakeremake.data.usertrack.sync

import android.util.Log
import com.simplyawakeremake.data.user.UserRepository
import com.simplyawakeremake.usecases.UserTrackLoginSyncUseCase
import kotlinx.coroutines.flow.first

class InitialUserTrackSyncManager(
    private val userRepository: UserRepository,
    private val loginSyncUseCase: UserTrackLoginSyncUseCase,
) {

    companion object {
        private const val TAG = "InitialUserTrackSyncMgr"
    }

    private var hasSyncedThisProcess: Boolean = false


    suspend fun runIfLoggedInAndNeeded(): UserTrackSyncResult? {
        if (hasSyncedThisProcess) {
            Log.d(TAG, "runIfLoggedInAndNeeded: already synced this process, skipping")
            return null
        }

        val user = userRepository.currentUser.first()
        if (user.firebaseUid == null) {
            Log.d(TAG, "runIfLoggedInAndNeeded: current user is guest, skipping")
            return null
        }

        hasSyncedThisProcess = true
        Log.i(TAG, "runIfLoggedInAndNeeded: starting initial UP+DOWN sync")
        return loginSyncUseCase.invoke()
    }
}
