package com.simplyawakeremake.data.usertrack


import android.util.Log
import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.data.common.mapData
import com.simplyawakeremake.data.track.Track
import com.simplyawakeremake.data.track.repository.TrackRepository
import com.simplyawakeremake.data.track.repository.TrackRepositoryInterface
import com.simplyawakeremake.data.track.withUserMeta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine

class TrackWithUserRepository(
    private val catalogRepo: TrackRepository,
    private val userTrackRepository: UserTrackRepository,
) : TrackRepositoryInterface {
    private val TAG = "TrackWithUserRepository"
    override fun getAllTracks(): Flow<ResultState<List<Track>>> {
        val safeUserFlow = userTrackRepository.observeAll()
            .catch { e ->
                Log.e(TAG, "User meta observation failed, using empty list, error: $e")
                emit(emptyList())
            }

        return combine(
            catalogRepo.getAllTracks(),
            safeUserFlow
        ) { trackResult, userEntities ->
            val userMap = userEntities.associateBy { it.trackId }

            trackResult.mapData { tracks ->
                tracks.map { track ->
                    val meta = userMap[track.id]
                    track.withUserMeta(meta)
                }
            }
        }
    }

    override fun getTrackBy(id: String): Flow<ResultState<Track>> {
        return combine(
            catalogRepo.getTrackBy(id),
            userTrackRepository.observeTrack(id)
        ) { trackResult, userMeta ->
            trackResult.mapData { track ->
                track.withUserMeta(userMeta)
            }
        }
    }
}
