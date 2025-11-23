package com.simplyawakeremake.data.track.local

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val createDate: Int,
    val updateDate: Int,
    val name: String,
    val lengthInSeconds: Int,
    val tagString: String,
    val season: Int,
    val year: Int,
    val duration: String,
)