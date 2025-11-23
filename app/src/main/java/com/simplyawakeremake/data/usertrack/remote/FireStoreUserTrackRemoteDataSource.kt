package com.simplyawakeremake.data.usertrack.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.simplyawakeremake.data.FirestoreCollectionNames
import com.simplyawakeremake.data.usertrack.UserTrack
import com.simplyawakeremake.data.usertrack.toDomain
import com.simplyawakeremake.data.usertrack.toDto
import kotlinx.coroutines.tasks.await

class FirestoreUserTrackRemoteDataSource(
    private val firestore: FirebaseFirestore,
) : UserTrackRemoteDataSource {

    override suspend fun upsertUserTrack(userTrack: UserTrack) {
        val dto = userTrack.toDto()

        firestore.collection(FirestoreCollectionNames.COLLECTON_USERS)
            .document(userTrack.userId)
            .collection(FirestoreCollectionNames.COLLECTON_TRACKS)
            .document(userTrack.trackId)
            .set(dto, SetOptions.merge())
            .await()
    }

    override suspend fun fetchAllForUser(userId: String): List<UserTrack> {
        val snapshot = firestore.collection(FirestoreCollectionNames.COLLECTON_USERS)
            .document(userId)
            .collection(FirestoreCollectionNames.COLLECTON_TRACKS)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(UserTrackDto::class.java)?.toDomain(userId)
        }
    }
}
