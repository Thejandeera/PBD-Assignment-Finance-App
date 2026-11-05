package com.clarity.finance.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.clarity.finance.data.local.UserPreferencesRepository
import com.clarity.finance.data.repository.DraftRepository
import com.clarity.finance.util.BankSmsParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Listens to incoming bank SMS/app notifications (when the user grants Notification Access)
 * and drafts recognized transactions for review without requiring READ_SMS permission.
 */
@AndroidEntryPoint
class BankNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var draftRepository: DraftRepository

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastProcessedSignature: String? = null

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName == packageName) return
        if (!userPreferencesRepository.isSmsScanEnabled) return

        val extras = sbn.notification?.extras ?: return
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val body = (
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
            )?.toString().orEmpty()

        if (body.isBlank()) return

        val signature = "${sbn.packageName}|$sender|$body"
        if (signature == lastProcessedSignature) return

        val parsed = BankSmsParser.parseSms(sender, body, sbn.postTime) ?: return
        lastProcessedSignature = signature

        serviceScope.launch {
            runCatching {
                draftRepository.saveDraft(parsed)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
