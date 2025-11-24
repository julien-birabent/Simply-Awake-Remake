package com.simplyawakeremake.data.user

data class User(
    val id: String,
    val firebaseUid: String?,
    val email: String?,
    val displayName: String?,
    val createdAt: Long,
    val lastActiveAt: Long
)