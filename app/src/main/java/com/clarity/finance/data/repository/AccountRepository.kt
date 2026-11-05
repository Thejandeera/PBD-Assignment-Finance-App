package com.clarity.finance.data.repository

import com.clarity.finance.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>
    suspend fun getAccount(accountId: String): Account?
    suspend fun addAccount(account: Account): Result<String>
    suspend fun updateAccount(account: Account): Result<Unit>
    suspend fun deleteAccount(accountId: String): Result<Unit>
    suspend fun adjustBalance(accountId: String, deltaInAccountCurrency: Double): Result<Unit>
}
