package com.clarity.finance.model

/**
 * The signed-in user as the rest of the app sees it.
 * Keeps FirebaseUser out of ViewModels and Composables.
 */
data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String
)
