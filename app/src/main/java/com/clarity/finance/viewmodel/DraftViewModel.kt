package com.clarity.finance.viewmodel

import android.content.Context
import android.provider.Telephony
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clarity.finance.data.repository.DraftRepository
import com.clarity.finance.data.repository.ExpenseRepository
import com.clarity.finance.model.DraftTransaction
import com.clarity.finance.model.DraftTransactionType
import com.clarity.finance.model.Expense
import com.clarity.finance.model.SpendingType
import com.clarity.finance.model.getDefaultCategories
import com.clarity.finance.util.BankSmsParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

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
     * Reads the real SMS inbox on physical devices when READ_SMS permission is granted.
     */
    fun scanDeviceInbox(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, feedbackMessage = null) }
            try {
                val cursor = context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    arrayOf(Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.DATE),
                    null,
                    null,
                    "${Telephony.Sms.Inbox.DATE} DESC LIMIT 50"
                )

                val detectedDrafts = mutableListOf<DraftTransaction>()
                cursor?.use { c ->
                    val addressIdx = c.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
                    val bodyIdx = c.getColumnIndex(Telephony.Sms.Inbox.BODY)
                    val dateIdx = c.getColumnIndex(Telephony.Sms.Inbox.DATE)

                    while (c.moveToNext()) {
                        val sender = if (addressIdx != -1) c.getString(addressIdx) else ""
                        val body = if (bodyIdx != -1) c.getString(bodyIdx) else ""
                        val date = if (dateIdx != -1) c.getLong(dateIdx) else System.currentTimeMillis()

                        val parsed = BankSmsParser.parseSms(sender, body, date)
                        if (parsed != null) {
                            detectedDrafts.add(parsed)
                        }
                    }
                }

                if (detectedDrafts.isNotEmpty()) {
                    draftRepository.saveDrafts(detectedDrafts)
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            feedbackMessage = "Found and drafted ${detectedDrafts.size} bank transactions!"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            feedbackMessage = "No new bank transaction SMS detected in inbox."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        feedbackMessage = "Could not read SMS: ${e.message}"
                    )
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
            _uiState.update {
                it.copy(
                    isScanning = false,
                    feedbackMessage = "Imported ${parsedList.size} sample bank SMS drafts (BOC & COMBANK)!"
                )
            }
        }
    }

    fun approveDraft(
        draft: DraftTransaction,
        selectedCategoryId: String? = null,
        selectedCategoryName: String? = null,
        spendingType: SpendingType? = null
    ) {
        viewModelScope.launch {
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
                    date = draft.date,
                    month = draft.date.take(7),
                    note = "Auto-Drafted from ${draft.sender}: ${draft.merchantOrTitle}",
                    createdAt = System.currentTimeMillis()
                )
                expenseRepository.addExpense(expense)
            }

            draftRepository.markDraftApproved(draft.id)
            _uiState.update { it.copy(feedbackMessage = "Approved: ${draft.merchantOrTitle} (LKR ${draft.amount.toLong()})") }
        }
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
