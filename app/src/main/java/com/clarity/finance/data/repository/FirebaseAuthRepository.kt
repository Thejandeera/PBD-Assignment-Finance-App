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

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
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

    override suspend fun signUp(name: String, email: String, password: String): Result<AuthUser> =
        runCatching {
            val user = auth.createUserWithEmailAndPassword(email, password).await().user
                ?: error("Sign-up returned no user")

            val nameUpdate = UserProfileChangeRequest.Builder().setDisplayName(name).build()
            user.updateProfile(nameUpdate).await()

            createProfileIfMissing(user, name)
            user.toAuthUser().copy(displayName = name)
        }

    override suspend fun signIn(email: String, password: String): Result<AuthUser> =
        runCatching {
            val user = auth.signInWithEmailAndPassword(email, password).await().user
                ?: error("Sign-in returned no user")
            user.toAuthUser()
        }

    override fun signOut() = auth.signOut()

    /**
     * Writes users/{uid} and users/{uid}/goals/primary_goal in one batch, so a new
     * account never ends up with a profile but no goal (or the other way round).
     * Skips the write if the profile already exists.
     */
    private suspend fun createProfileIfMissing(user: FirebaseUser, name: String) {
        val userDoc = firestore.collection(FirestorePaths.USERS).document(user.uid)
        if (userDoc.get().await().exists()) return

        val profile = UserProfile(name = name, email = user.email.orEmpty())
        val profileData = mapOf(
            "name" to profile.name,
            "email" to profile.email,
            "baseCurrency" to profile.baseCurrency,
            "rates" to profile.rates,
            "createdAt" to FieldValue.serverTimestamp()
        )

        // A new user has saved nothing in the app yet, so startingAmount is 0.
        // The goal name and target are only a starting point the user edits on the Goal screen.
        val defaultGoal = Goal(
            startingAmount = 0.0,
            deadline = LocalDate.now().plusYears(1).toString()
        )
        val goalData = mapOf(
            "name" to defaultGoal.name,
            "targetAmount" to defaultGoal.targetAmount,
            "startingAmount" to defaultGoal.startingAmount,
            "deadline" to defaultGoal.deadline,
            "createdAt" to FieldValue.serverTimestamp()
        )
        val goalDoc = userDoc.collection(FirestorePaths.GOALS).document(FirestorePaths.PRIMARY_GOAL_ID)

        firestore.batch()
            .set(userDoc, profileData)
            .set(goalDoc, goalData)
            .commit()
            .await()
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email.orEmpty(),
        displayName = displayName.orEmpty()
    )
}
