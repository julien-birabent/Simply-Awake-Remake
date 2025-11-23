package com.simplyawakeremake.data.usertrack.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.simplyawakeremake.data.FirestoreCollectionNames
import com.simplyawakeremake.data.usertrack.local.UserTrackEntity
import kotlinx.coroutines.tasks.await


class FirestoreUserTrackRemoteDataSource(
    private val firestore: FirebaseFirestore,
) : UserTrackRemoteDataSource {

    override suspend fun upsertUserTrack(entity: UserTrackEntity) {
        val now = System.currentTimeMillis()
        val remote = entity.toDto(now)

        firestore.collection(FirestoreCollectionNames.COLLECTON_USERS)
            .document(entity.userId)
            .collection(FirestoreCollectionNames.COLLECTON_TRACKS)
            .document(entity.trackId)
            .set(remote, SetOptions.merge())
            .await()
    }

    override suspend fun fetchAllForUser(userId: String): List<UserTrackDto> {
        val snapshot = firestore.collection(FirestoreCollectionNames.COLLECTON_USERS)
            .document(userId)
            .collection(FirestoreCollectionNames.COLLECTON_TRACKS)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(UserTrackDto::class.java)
        }
    }
}
