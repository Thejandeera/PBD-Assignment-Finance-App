package com.clarity.finance.data.repository

import com.clarity.finance.model.DraftTransaction
import kotlinx.coroutines.flow.Flow

interface DraftRepository {
    val pendingDraftCount: Flow<Int>
    fun observePendingDrafts(): Flow<List<DraftTransaction>>
    suspend fun saveDraft(draft: DraftTransaction): Result<Unit>
    suspend fun saveDrafts(drafts: List<DraftTransaction>): Result<Unit>
    suspend fun discardDraft(draftId: String): Result<Unit>
    suspend fun markDraftApproved(draftId: String): Result<Unit>
    suspend fun clearAllDrafts(): Result<Unit>
}
