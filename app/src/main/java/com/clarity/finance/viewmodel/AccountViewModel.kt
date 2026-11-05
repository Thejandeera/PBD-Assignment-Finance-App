package com.clarity.finance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clarity.finance.data.local.PrimaryAccountProvider
import com.clarity.finance.data.repository.AccountRepository
import com.clarity.finance.data.repository.ExchangeRateProvider
import com.clarity.finance.model.Account
import com.clarity.finance.model.AccountBalanceCalculator
import com.clarity.finance.model.AccountType
import com.clarity.finance.model.CurrencyType
import com.clarity.finance.util.CurrencyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountUiState(
    val isLoading: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val primaryAccountId: String? = null,
    val totalBalanceLkr: Double = 0.0,
    val lkrPerUsdRate: Double = CurrencyFormatter.FALLBACK_LKR_PER_USD,
    val errorMessage: String? = null
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val exchangeRateProvider: ExchangeRateProvider,
    private val primaryAccountProvider: PrimaryAccountProvider
) : ViewModel() {

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val uiState: StateFlow<AccountUiState> = combine(
        accountRepository.observeAccounts(),
        exchangeRateProvider.observeLkrPerUsdRate(),
        primaryAccountProvider.primaryAccountIdFlow
    ) { accounts, rate, primaryId ->
        val totalLkr = accounts.sumOf { account ->
            AccountBalanceCalculator.toLkr(account.currentBalance, account.currency, rate)
        }
        val sorted = accounts.sortedByDescending { it.id == primaryId }
        AccountUiState(
            isLoading = false,
            accounts = sorted,
            primaryAccountId = primaryId,
            totalBalanceLkr = totalLkr,
            lkrPerUsdRate = rate,
            errorMessage = null
        )
    }.catch { error ->
        emit(
            AccountUiState(
                isLoading = false,
                errorMessage = error.message ?: "Couldn't load accounts."
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountUiState(isLoading = true)
    )

    fun validateAccountInput(name: String, balanceInput: String): Pair<String?, String?> {
        val nameError = if (name.trim().isEmpty()) "Enter an account name" else null
        val balance = balanceInput.trim().toDoubleOrNull()
        val balanceError = if (balance == null || !balance.isFinite()) {
            "Enter a valid balance"
        } else {
            null
        }
        return nameError to balanceError
    }

    fun addAccount(
        name: String,
        type: AccountType,
        currency: CurrencyType,
        balanceInput: String,
        isPrimary: Boolean = false,
        onSuccess: (() -> Unit)? = null
    ): Boolean {
        val (nameErr, balErr) = validateAccountInput(name, balanceInput)
        if (nameErr != null || balErr != null) {
            _userMessage.value = nameErr ?: balErr
            return false
        }
        val balance = balanceInput.trim().toDouble()
        val resolvedCurrency = if (currency == CurrencyType.USD) CurrencyType.USD else CurrencyType.LKR
        viewModelScope.launch {
            accountRepository.addAccount(
                Account(
                    name = name.trim(),
                    type = type,
                    currency = resolvedCurrency,
                    currentBalance = balance
                )
            ).onSuccess { newId ->
                if (isPrimary) {
                    primaryAccountProvider.setPrimaryAccountId(newId)
                }
                _userMessage.value = "${name.trim()} added"
                onSuccess?.invoke()
            }.onFailure { err ->
                _userMessage.value = err.message ?: "Couldn't add account"
            }
        }
        return true
    }

    fun updateAccount(
        existing: Account,
        name: String,
        type: AccountType,
        currency: CurrencyType,
        balanceInput: String,
        isPrimary: Boolean? = null,
        onSuccess: (() -> Unit)? = null
    ): Boolean {
        val (nameErr, balErr) = validateAccountInput(name, balanceInput)
        if (nameErr != null || balErr != null) {
            _userMessage.value = nameErr ?: balErr
            return false
        }
        val balance = balanceInput.trim().toDouble()
        val resolvedCurrency = if (currency == CurrencyType.USD) CurrencyType.USD else CurrencyType.LKR
        viewModelScope.launch {
            accountRepository.updateAccount(
                existing.copy(
                    name = name.trim(),
                    type = type,
                    currency = resolvedCurrency,
                    currentBalance = balance
                )
            ).onSuccess {
                if (isPrimary == true) {
                    primaryAccountProvider.setPrimaryAccountId(existing.id)
                } else if (isPrimary == false) {
                    primaryAccountProvider.clearPrimaryAccountIfMatches(existing.id)
                }
                _userMessage.value = "${name.trim()} updated"
                onSuccess?.invoke()
            }.onFailure { err ->
                _userMessage.value = err.message ?: "Couldn't update account"
            }
        }
        return true
    }

    fun togglePrimaryAccount(account: Account) {
        val wasPrimary = primaryAccountProvider.primaryAccountIdFlow.value == account.id
        primaryAccountProvider.togglePrimaryAccount(account.id)
        _userMessage.value = if (wasPrimary) {
            "${account.name} is no longer primary"
        } else {
            "${account.name} set as primary"
        }
    }

    fun deleteAccount(account: Account, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            accountRepository.deleteAccount(account.id)
                .onSuccess {
                    primaryAccountProvider.clearPrimaryAccountIfMatches(account.id)
                    _userMessage.value = "${account.name} deleted"
                    onSuccess?.invoke()
                }
                .onFailure { err ->
                    _userMessage.value = err.message ?: "Couldn't delete account"
                }
        }
    }

    fun onUserMessageShown() {
        _userMessage.value = null
    }
}
