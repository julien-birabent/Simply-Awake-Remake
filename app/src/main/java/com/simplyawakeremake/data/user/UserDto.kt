package com.simplyawakeremake.data.user


data class UserDto(
    val id: String,
    val firebaseUid: String?,
    val email: String?,
    val displayName: String?,
    val createdAt: Long,
    val lastActiveAt: Long,
)

fun User.toRemoteDto(): UserDto =
    UserDto(
        id = id,
        firebaseUid = firebaseUid,
        email = email,
        displayName = displayName,
        createdAt = createdAt,
        lastActiveAt = lastActiveAt,
    )

fun UserDto.toDomain(): User =
    User(
        id = id,
        firebaseUid = firebaseUid,
        email = email,
        displayName = displayName,
        createdAt = createdAt,
        lastActiveAt = lastActiveAt,
    )