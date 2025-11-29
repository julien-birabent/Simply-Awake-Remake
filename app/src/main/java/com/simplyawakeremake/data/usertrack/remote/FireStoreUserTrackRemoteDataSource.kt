package com.simplyawakeremake.data.usertrack.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.simplyawakeremake.data.FirestoreCollectionNames
import com.simplyawakeremake.data.usertrack.UserTrack
import kotlinx.coroutines.tasks.await

class FirestoreUserTrackRemoteDataSource(
    private val firestore: FirebaseFirestore,
) : UserTrackRemoteDataSource {

    override suspend fun upsertUserTrack(remoteUserId: String, userTrack: UserTrack) {
        val dto = userTrack.toDto()

        firestore.collection(FirestoreCollectionNames.COLLECTION_USERS)
            .document(remoteUserId)
            .collection(FirestoreCollectionNames.COLLECTION_TRACKS)
            .document(userTrack.trackId)
            .set(dto, SetOptions.merge())
            .await()
    }

    override suspend fun fetchAllForUser(remoteUserId: String): List<UserTrack> {
        val snapshot = firestore.collection(FirestoreCollectionNames.COLLECTION_USERS)
            .document(remoteUserId)
            .collection(FirestoreCollectionNames.COLLECTION_TRACKS)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(UserTrackDto::class.java)?.toDomain()
        }
    }
}
