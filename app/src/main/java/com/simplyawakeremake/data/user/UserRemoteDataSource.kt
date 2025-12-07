package com.simplyawakeremake.data.user

interface UserRemoteDataSource {
    suspend fun upsertUser(user: User)
    suspend fun fetchUser(userId: String): User?
}