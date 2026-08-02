package com.example.autoresponder.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.autoresponder.data.AppDatabase
import com.example.autoresponder.data.AppSettings
import com.example.autoresponder.data.ReplyType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * بيسمع أي إشعار جديد من تطبيق ماسنجر (com.facebook.orca)
 * ويستخرج اسم المرسل ونص الرسالة، وبعدين يقرر هل يرد ولا لأ حسب القواعد المخزنة.
 */
class MessengerNotificationListener : NotificationListenerService() {

    companion object {
        private const val MESSENGER_PACKAGE = "com.facebook.orca"
        private const val TAG = "AutoResponder"
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != MESSENGER_PACKAGE) return
        if (!AppSettings.isServiceEnabled(applicationContext)) return

        val extras = sbn.notification.extras
        val senderName = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val messageText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        // تجاهل إشعارات المجموعات الملخصة أو الفاضية
        if (senderName.isBlank()) return

        Log.d(TAG, "رسالة جديدة من: $senderName")

        scope.launch {
            handleIncomingMessage(senderName, messageText)
        }
    }

    private suspend fun handleIncomingMessage(senderName: String, messageText: String) {
        val dao = AppDatabase.getInstance(applicationContext).ruleDao()
        val rule = dao.getRuleForSender(senderName) ?: dao.getDefaultRule() ?: return

        if (!rule.isEnabled) return

        // التحقق من وقت التفعيل
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (currentHour < rule.activeFromHour || currentHour >= rule.activeToHour) return

        val replyText = when (rule.replyType) {
            ReplyType.FIXED_TEXT -> rule.fixedReplyText
            ReplyType.AI_SMART -> {
                val apiKey = AppSettings.getApiKey(applicationContext)
                if (apiKey.isBlank()) rule.fixedReplyText
                else ClaudeApiClient.generateReply(apiKey, senderName, messageText)
            }
        }

        if (replyText.isNotBlank()) {
            // تأخير عشوائي بسيط عشان الرد يبان طبيعي أكتر
            kotlinx.coroutines.delay((2000..7000).random().toLong())
            ReplyAccessibilityService.pendingReply = PendingReply(senderName, replyText)
            ReplyAccessibilityService.requestOpenConversation(applicationContext, senderName)
        }
    }
}

data class PendingReply(val senderName: String, val text: String)
