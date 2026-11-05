package com.clarity.finance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clarity.finance.data.local.PrimaryAccountProvider
import com.clarity.finance.data.repository.AccountRepository
import com.clarity.finance.data.repository.DraftRepository
import com.clarity.finance.data.repository.ExchangeRateProvider
import com.clarity.finance.data.repository.ExpenseRepository
import com.clarity.finance.data.repository.IncomeRepository
import com.clarity.finance.model.Account
import com.clarity.finance.model.AccountBalanceCalculator
import com.clarity.finance.model.CurrencyType
import com.clarity.finance.model.DraftTransaction
import com.clarity.finance.model.DraftTransactionType
import com.clarity.finance.model.Expense
import com.clarity.finance.model.Income
import com.clarity.finance.model.IncomeSource
import com.clarity.finance.model.IncomeStatus
import com.clarity.finance.model.SpendingType
import com.clarity.finance.model.getDefaultCategories
import com.clarity.finance.util.BankSmsParser
import com.clarity.finance.util.CurrencyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class DraftUiState(
    val drafts: List<DraftTransaction> = emptyList(),
    val isScanning: Boolean = false,
    val feedbackMessage: String? = null
)

@HiltViewModel
class DraftViewModel @Inject constructor(
    private val draftRepository: DraftRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val accountRepository: AccountRepository,
    private val exchangeRateProvider: ExchangeRateProvider,
    private val primaryAccountProvider: PrimaryAccountProvider
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.observeAccounts()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(DraftUiState())
    val uiState: StateFlow<DraftUiState> = _uiState.asStateFlow()

    val pendingDraftCount: StateFlow<Int> = draftRepository.pendingDraftCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    init {
        viewModelScope.launch {
            draftRepository.observePendingDrafts().collect { list ->
                _uiState.update { it.copy(drafts = list) }
            }
        }
    }

    /**
     * Parses a bank SMS pasted by the user without requiring READ_SMS permission.
     */
    fun parsePastedSms(body: String, sender: String = "BANK") {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(feedbackMessage = "Paste a bank SMS message first.") }
            return
        }
        viewModelScope.launch {
            val parsed = BankSmsParser.parseSms(sender, trimmed)
            if (parsed != null) {
                draftRepository.saveDrafts(listOf(parsed))
                    .onSuccess {
                        _uiState.update {
                            it.copy(feedbackMessage = "Drafted ${parsed.merchantOrTitle} (${parsed.amount} ${parsed.currency}).")
                        }
                    }
                    .onFailure { err ->
                        _uiState.update {
                            it.copy(feedbackMessage = "Failed to save draft: ${err.message}")
                        }
                    }
            } else {
                _uiState.update {
                    it.copy(feedbackMessage = "Could not detect a BOC or ComBank transaction in that message.")
                }
            }
        }
    }

    /**
     * Injects the preloaded sample bank SMS messages from the user's images (BOC, COMBANK).
     * Ideal for quick testing on Emulator and Presentation Live Demo.
     */
    fun loadSampleSmsDrafts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, feedbackMessage = null) }
            val samples = BankSmsParser.getSampleBankSmsList()
            val parsedList = samples.mapNotNull { (sender, body) ->
                BankSmsParser.parseSms(sender, body)
            }
            draftRepository.saveDrafts(parsedList)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            feedbackMessage = "Imported ${parsedList.size} sample bank SMS drafts (BOC & COMBANK)!"
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            feedbackMessage = "Failed to import sample drafts: ${err.message}"
                        )
                    }
                }
        }
    }

    fun approveDraft(
        draft: DraftTransaction,
        selectedCategoryId: String? = null,
        selectedCategoryName: String? = null,
        spendingType: SpendingType? = null,
        selectedAccount: Account? = null
    ) {
        viewModelScope.launch {
            val resolvedAccount = selectedAccount ?: suggestAccountForDraft(draft)
            val rate = exchangeRateProvider.getLkrPerUsdRate()

            if (draft.type == DraftTransactionType.EXPENSE) {
                val catId = selectedCategoryId ?: draft.suggestedCategoryId
                val catName = selectedCategoryName ?: draft.suggestedCategoryName
                val sType = spendingType ?: draft.spendingType

                val categoryItem = getDefaultCategories().firstOrNull { it.id == catId }
                val icon = categoryItem?.icon ?: "💳"

                val expense = Expense(
                    id = UUID.randomUUID().toString(),
                    amount = draft.amount,
                    categoryId = catId,
                    categoryName = catName,
                    categoryIcon = icon,
                    type = sType,
                    paymentMethod = draft.paymentMethod,
                    accountId = resolvedAccount?.id,
                    accountName = resolvedAccount?.name,
                    date = draft.date,
                    month = draft.date.take(7),
                    note = "Auto-Drafted from ${draft.sender}: ${draft.merchantOrTitle}",
                    createdAt = System.currentTimeMillis()
                )
                val addResult = expenseRepository.addExpense(expense)
                if (addResult.isSuccess && resolvedAccount != null) {
                    val delta = AccountBalanceCalculator.expenseDelta(draft.amount, resolvedAccount.currency, rate)
                    accountRepository.adjustBalance(resolvedAccount.id, delta)
                }
            } else {
                val income = Income(
                    id = UUID.randomUUID().toString(),
                    sourceId = IncomeSource.FREELANCE.name,
                    sourceName = IncomeSource.FREELANCE.displayName,
                    sourceIcon = IncomeSource.FREELANCE.icon,
                    amount = draft.amount,
                    currency = CurrencyType.LKR,
                    exchangeRate = 1.0,
                    amountLKR = draft.amount,
                    status = IncomeStatus.RECEIVED,
                    date = draft.date,
                    month = draft.date.take(7),
                    projectName = draft.merchantOrTitle,
                    note = "Auto-Drafted from ${draft.sender}",
                    accountId = resolvedAccount?.id,
                    accountName = resolvedAccount?.name,
                    createdAt = System.currentTimeMillis()
                )
                val addResult = incomeRepository.addIncome(income)
                if (addResult.isSuccess && resolvedAccount != null) {
                    val delta = AccountBalanceCalculator.incomeDelta(income, resolvedAccount.currency, rate)
                    accountRepository.adjustBalance(resolvedAccount.id, delta)
                }
            }

            draftRepository.markDraftApproved(draft.id)
            _uiState.update { it.copy(feedbackMessage = "Approved: ${draft.merchantOrTitle} (LKR ${draft.amount.toLong()})") }
        }
    }

    fun suggestAccountForDraft(draft: DraftTransaction): Account? {
        val list = accounts.value
        if (list.isEmpty()) return null
        val senderUpper = draft.sender.uppercase()
        return list.firstOrNull { acc ->
            val nameUpper = acc.name.uppercase()
            (senderUpper.contains("COMBANK") && (nameUpper.contains("COMMERCIAL") || nameUpper.contains("COMBANK"))) ||
                (senderUpper.contains("BOC") && (nameUpper.contains("BOC") || nameUpper.contains("CEYLON"))) ||
                nameUpper.contains(senderUpper)
        } ?: AccountBalanceCalculator.resolveDefaultAccount(list, primaryAccountProvider.primaryAccountIdFlow.value)
    }

    fun approveAll() {
        val currentDrafts = _uiState.value.drafts
        viewModelScope.launch {
            currentDrafts.forEach { draft ->
                approveDraft(draft)
            }
            _uiState.update { it.copy(feedbackMessage = "All ${currentDrafts.size} drafts approved and committed!") }
        }
    }

    fun discardDraft(draftId: String) {
        viewModelScope.launch {
            draftRepository.discardDraft(draftId)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            draftRepository.clearAllDrafts()
            _uiState.update { it.copy(feedbackMessage = "All drafts cleared.") }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
