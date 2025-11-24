package com.simplyawakeremake.data.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey val id: String,
    val firebaseUid: String?,
    val email: String?,
    val displayName: String?,
    val createdAt: Long,
    val lastActiveAt: Long,
)

fun UserEntity.toDomain(): User =
    User(
        id = id,
        firebaseUid = firebaseUid,
        email = email,
        displayName = displayName,
        createdAt = createdAt,
        lastActiveAt = lastActiveAt
    )

fun User.toEntity(): UserEntity =
    UserEntity(
        id = id,
        firebaseUid = firebaseUid,
        email = email,
        displayName = displayName,
        createdAt = createdAt,
        lastActiveAt = lastActiveAt
    )