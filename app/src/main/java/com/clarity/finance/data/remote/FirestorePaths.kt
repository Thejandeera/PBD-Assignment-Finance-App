package com.clarity.finance.data.remote

/**
 * Collection names used in Firestore. Every user's data lives under users/{uid},
 * which is what firestore.rules protects.
 */
object FirestorePaths {
    const val USERS = "users"
    const val INCOMES = "incomes"
    const val EXPENSES = "expenses"
    const val RECURRING = "recurring"
    const val GOALS = "goals"
    const val SAVINGS = "savings"
    const val CATEGORIES = "categories"

    const val PRIMARY_GOAL_ID = "primary_goal"
}
