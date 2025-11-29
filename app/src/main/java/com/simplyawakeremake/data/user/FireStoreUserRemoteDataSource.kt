package com.simplyawakeremake.data.user

import com.google.firebase.firestore.FirebaseFirestore
import com.simplyawakeremake.data.FirestoreCollectionNames
import kotlinx.coroutines.tasks.await

class FirestoreUserRemoteDataSource(
    private val firestore: FirebaseFirestore,
) : UserRemoteDataSource {

    override suspend fun upsertUser(user: User) {
        val dto = user.toRemoteDto()

        firestore
            .collection(FirestoreCollectionNames.COLLECTION_USERS)
            .document(user.firebaseUid!!)
            .set(dto)
            .await()
    }

    override suspend fun fetchUser(userFirebaseId: String): User? {
        val snapshot = firestore
            .collection(FirestoreCollectionNames.COLLECTION_USERS)
            .document(userFirebaseId)
            .get()
            .await()

        return snapshot.toObject(UserDto::class.java)?.toDomain()
    }
}