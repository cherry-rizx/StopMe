package com.hanyz.stopme.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String?
)

object AuthRepository {

    private fun getAuth(): FirebaseAuth? {
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    val currentUser: UserProfile?
        get() {
            val user = getAuth()?.currentUser ?: return null
            return UserProfile(
                uid = user.uid,
                name = user.displayName ?: user.email?.substringBefore("@") ?: "Pengguna StopMe",
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString()
            )
        }

    fun isUserLoggedIn(): Boolean {
        return getAuth()?.currentUser != null
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser?> {
        val auth = getAuth() ?: return Result.failure(Exception("Firebase Auth belum diinisialisasi"))
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String): Result<FirebaseUser?> {
        val auth = getAuth() ?: return Result.failure(Exception("Firebase Auth belum diinisialisasi"))
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Kirim email untuk mengatur ulang kata sandi
    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val auth = getAuth() ?: return Result.failure(Exception("Firebase Auth belum diinisialisasi"))
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleCredentialManager(
        context: Context,
        serverClientId: String
    ): Result<FirebaseUser?> {
        val auth = getAuth() ?: return Result.failure(Exception("Firebase Auth belum diinisialisasi"))
        val credentialManager = CredentialManager.create(context)

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(context, request)
            val credential = response.credential

            if (credential is androidx.credentials.CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                Result.success(authResult.user)
            } else {
                Result.failure(Exception("Kredensial tidak valid"))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Biarkan pembatalan (mis. batas waktu) diteruskan ke pemanggil
            throw e
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Login Firebase memakai ID token Google (dipakai jalur cadangan metode lama)
    suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser?> {
        val auth = getAuth() ?: return Result.failure(Exception("Firebase Auth belum diinisialisasi"))
        return try {
            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(authCredential).await()
            Result.success(authResult.user)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            getAuth()?.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
