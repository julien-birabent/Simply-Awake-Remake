package com.simplyawakeremake.data.usertrack


import android.util.Log
import com.simplyawakeremake.data.audioquality.AudioQualityRepository
import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.data.common.mapData
import com.simplyawakeremake.data.track.Track
import com.simplyawakeremake.data.track.repository.TrackRepository
import com.simplyawakeremake.data.track.repository.TrackRepositoryInterface
import com.simplyawakeremake.data.track.withAudioQuality
import com.simplyawakeremake.data.track.withUserMeta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine

class FullTrackRepository(
    private val catalogRepo: TrackRepository,
    private val userTrackRepository: UserTrackRepository,
    private val audioQualityRepository: AudioQualityRepository,
) : TrackRepositoryInterface {
    private val TAG = "TrackWithUserRepository"

    override fun getAllTracks(): Flow<ResultState<List<Track>>> {
        val safeUserFlow: Flow<List<UserTrack>> =
            userTrackRepository.observeAll()
                .catch { e ->
                    Log.e(TAG, "User meta observation failed, using empty list", e)
                    emit(emptyList())
                }

        return combine(
            catalogRepo.getAllTracks(),
            safeUserFlow,
            audioQualityRepository.observeAll()
        ) { trackResult, userTracks, audioQualities ->
            val userMap: Map<String, UserTrack> = userTracks.associateBy { it.trackId }

            trackResult.mapData { tracks ->
                tracks.map { track ->
                    val meta = userMap[track.id]
                    track.withUserMeta(meta).withAudioQuality(audioQualities[track.id])
                }
            }
        }
    }


    override fun getTrackBy(id: String): Flow<ResultState<Track>> {
        return combine(
            catalogRepo.getTrackBy(id),
            userTrackRepository.observeTrack(id),
            audioQualityRepository.observeAll()
        ) { trackResult, userMeta, audioQualities ->
            trackResult.mapData { track ->
                track.withUserMeta(userMeta).withAudioQuality(audioQualities[track.id])
            }
        }
    }
}
