package com.simplyawakeremake.data.audioquality.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.simplyawakeremake.data.FirestoreCollectionNames
import com.simplyawakeremake.data.audioquality.AudioQualityLevel
import kotlinx.coroutines.tasks.await

class FirestoreAudioQualityRemoteDataSource(
    private val firestore: FirebaseFirestore,
) : AudioQualityRemoteDataSource {

    override suspend fun fetchAll(): Map<String, AudioQualityLevel> {
        val snapshot = firestore.collection(FirestoreCollectionNames.COLLECTION_AUDIO_QUALITY)
            .get()
            .await()

        return snapshot.documents
            .mapNotNull { doc ->
                AudioQualityLevel.fromName(doc.getString(FIELD_LEVEL))?.let { doc.id to it }
            }
            .toMap()
    }

    private companion object {
        const val FIELD_LEVEL = "level"
    }
}
