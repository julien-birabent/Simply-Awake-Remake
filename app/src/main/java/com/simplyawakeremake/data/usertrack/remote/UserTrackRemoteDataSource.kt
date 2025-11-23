package com.simplyawakeremake.data.usertrack.remote

interface UserTrackRemoteDataSource {

    suspend fun fetchAllForUser(userId: String): List<UserTrackDto>

    suspend fun updateFavorite(
        userId: String,
        trackId: String,
        isFavorite: Boolean
    )

    suspend fun registerPlay(
        userId: String,
        trackId: String,
        playedAtMillis: Long
    )
}