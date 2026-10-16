package com.clarity.finance.data.repository

import com.clarity.finance.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Emits the signed-in user, or null after sign-out. Emits the current value first. */
    val currentUserFlow: Flow<AuthUser?>

    /** Synchronous snapshot, used to pick the start screen (Login or Dashboard). */
    val currentUser: AuthUser?

    suspend fun signUp(name: String, email: String, password: String): Result<AuthUser>
    suspend fun signIn(email: String, password: String): Result<AuthUser>
    fun signOut()
}
