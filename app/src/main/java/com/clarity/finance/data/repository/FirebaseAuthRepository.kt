package com.clarity.finance.data.repository

import com.clarity.finance.data.remote.FirestorePaths
import com.clarity.finance.model.AuthUser
import com.clarity.finance.model.Goal
import com.clarity.finance.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

import com.clarity.finance.data.local.UserPreferencesRepository

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userPreferencesRepository: UserPreferencesRepository
) : AuthRepository {

    override val currentUserFlow: Flow<AuthUser?> = callbackFlow {
        // FirebaseAuth calls this once on registration and again on every sign-in / sign-out.
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toAuthUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override val currentUser: AuthUser?
        get() = auth.currentUser?.toAuthUser()

    override suspend fun signUp(name: String, email: String, password: String, photoPath: String?): Result<AuthUser> =
        runCatching {
            val user = auth.createUserWithEmailAndPassword(email, password).await().user
                ?: error("Sign-up returned no user")

            val nameUpdate = UserProfileChangeRequest.Builder().setDisplayName(name).build()
            user.updateProfile(nameUpdate).await()

            if (!photoPath.isNullOrBlank()) {
                userPreferencesRepository.profilePhotoPath = photoPath
            }

            createProfileIfMissing(user, name, photoPath)
            user.toAuthUser().copy(displayName = name, photoPath = photoPath)
        }

    override suspend fun signIn(email: String, password: String): Result<AuthUser> =
        runCatching {
            val user = auth.signInWithEmailAndPassword(email, password).await().user
                ?: error("Sign-in returned no user")
            user.toAuthUser()
        }

    override suspend fun updateProfile(name: String, photoPath: String?): Result<Unit> =
        runCatching {
            val user = auth.currentUser ?: error("No authenticated user")
            val nameUpdate = UserProfileChangeRequest.Builder().setDisplayName(name).build()
            user.updateProfile(nameUpdate).await()

            if (photoPath != null) {
                userPreferencesRepository.profilePhotoPath = photoPath
            }

            firestore.collection(FirestorePaths.USERS).document(user.uid)
                .update(
                    mapOf(
                        "name" to name,
                        "photoPath" to (photoPath ?: userPreferencesRepository.profilePhotoPath)
                    )
                ).await()
            Unit
        }

    override fun signOut() = auth.signOut()

    /**
     * Writes users/{uid} and users/{uid}/goals/primary_goal in one batch, so a new
     * account never ends up with a profile but no goal (or the other way round).
     * Skips the write if the profile already exists.
     */
    private suspend fun createProfileIfMissing(user: FirebaseUser, name: String, photoPath: String? = null) {
        val userDoc = firestore.collection(FirestorePaths.USERS).document(user.uid)
        if (userDoc.get().await().exists()) return

        val profile = UserProfile(name = name, email = user.email.orEmpty(), photoPath = photoPath)
        val profileData = mapOf(
            "name" to profile.name,
            "email" to profile.email,
            "baseCurrency" to profile.baseCurrency,
            "rates" to profile.rates,
            "photoPath" to (profile.photoPath ?: ""),
            "createdAt" to FieldValue.serverTimestamp()
        )

        userDoc.set(profileData).await()
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email.orEmpty(),
        displayName = displayName.orEmpty(),
        photoPath = userPreferencesRepository.profilePhotoPath
    )
}
