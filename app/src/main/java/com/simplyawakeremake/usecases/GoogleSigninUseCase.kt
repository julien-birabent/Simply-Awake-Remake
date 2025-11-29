package com.simplyawakeremake.usecases


import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.simplyawakeremake.data.user.LinkCurrentUserToFirebaseException
import com.simplyawakeremake.data.user.UserRepository
import kotlinx.coroutines.tasks.await

class GoogleSignInUseCase(
    private val googleSignInClient: GoogleSignInClient,
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
) {

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    suspend fun handleSignInResult(data: Intent?) {
        val account = try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            task.getResult(ApiException::class.java)
        } catch (e: Exception) {
            throw Exception("Google sign-in failed", e)
        }

        val idToken = account.idToken ?: throw IllegalStateException("Google ID token is null")

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = try {
            firebaseAuth.signInWithCredential(credential).await()
        } catch (e: Exception) {
            throw Exception("Firebase sign-in with Google credential failed", e)
        }

        val firebaseUser = authResult.user
            ?: throw IllegalStateException("Firebase user is null after sign-in")

        try {
            userRepository.linkCurrentUserToRemote(
                userRemoteId = firebaseUser.uid,
                email = firebaseUser.email,
                displayName = firebaseUser.displayName,
            )
        } catch (e: LinkCurrentUserToFirebaseException) {
            firebaseAuth.signOut()
            throw e
        }
    }
}
