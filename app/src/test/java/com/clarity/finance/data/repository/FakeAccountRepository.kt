package com.clarity.finance.data.repository

import com.clarity.finance.data.local.PrimaryAccountProvider
import com.clarity.finance.model.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class FakePrimaryAccountProvider : PrimaryAccountProvider {
    private val _primaryId = MutableStateFlow<String?>(null)
    override val primaryAccountIdFlow: StateFlow<String?> = _primaryId.asStateFlow()

    private val _defaultCurrency = MutableStateFlow("LKR")
    override val defaultCurrencyFlow: StateFlow<String> = _defaultCurrency.asStateFlow()

    fun setDefaultCurrency(currency: String) {
        _defaultCurrency.value = currency
    }

    override fun setPrimaryAccountId(accountId: String?) {
        _primaryId.value = accountId?.takeIf { it.isNotBlank() }
    }

    override fun togglePrimaryAccount(accountId: String) {
        _primaryId.value = if (_primaryId.value == accountId) null else accountId
    }

    override fun clearPrimaryAccountIfMatches(accountId: String) {
        if (_primaryId.value == accountId) _primaryId.value = null
    }
}

class FakeAccountRepository : AccountRepository {

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accountsState = _accounts.asStateFlow()

    fun setAccounts(list: List<Account>) {
        _accounts.value = list
    }

    override fun observeAccounts(): Flow<List<Account>> = _accounts.asStateFlow()

    override suspend fun getAccount(accountId: String): Account? =
        _accounts.value.firstOrNull { it.id == accountId }

    override suspend fun addAccount(account: Account): Result<String> {
        val id = account.id.ifBlank { UUID.randomUUID().toString() }
        _accounts.value = _accounts.value + account.copy(id = id)
        return Result.success(id)
    }

    override suspend fun updateAccount(account: Account): Result<Unit> {
        _accounts.value = _accounts.value.map { if (it.id == account.id) account else it }
        return Result.success(Unit)
    }

    override suspend fun deleteAccount(accountId: String): Result<Unit> {
        _accounts.value = _accounts.value.filterNot { it.id == accountId }
        return Result.success(Unit)
    }

    override suspend fun adjustBalance(accountId: String, deltaInAccountCurrency: Double): Result<Unit> {
        _accounts.value = _accounts.value.map { acc ->
            if (acc.id == accountId) acc.copy(currentBalance = acc.currentBalance + deltaInAccountCurrency)
            else acc
        }
        return Result.success(Unit)
    }
}
