package com.simplyawakeremake.data.track.local


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