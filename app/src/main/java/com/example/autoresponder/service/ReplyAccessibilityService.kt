package com.example.autoresponder.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * بيتفعل لما ماسنجر يتفتح، بيدور على حقل كتابة الرسالة وزرار الإرسال
 * ويحاكي الكتابة والضغط عشان يبعت الرد المطلوب.
 *
 * ملحوظة: العناصر (view IDs) دي بتتغير مع تحديثات ماسنجر، فممكن تحتاج
 * تظبطها بنفسك لو التطبيق اتحدث. أنصحك تستخدم "Layout Inspector" في
 * Android Studio أو أداة uiautomatorviewer لمعرفة الـ IDs الحالية.
 */
class ReplyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoResponderA11y"
        var pendingReply: PendingReply? = null

        fun requestOpenConversation(context: Context, senderName: String) {
            val intent = context.packageManager.getLaunchIntentForPackage("com.facebook.orca")
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent?.let { context.startActivity(it) }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val reply = pendingReply ?: return
        if (event?.packageName != "com.facebook.orca") return

        val root = rootInActiveWindow ?: return

        // لازم تتأكد إننا في شاشة المحادثة الصحيحة قبل الكتابة (تحقق من العنوان)
        if (!isCorrectConversation(root, reply.senderName)) return

        val editText = findEditableNode(root) ?: return
        val sendButton = findSendButtonNode(root)

        setTextInNode(editText, reply.text)

        // انتظار بسيط قبل الضغط على إرسال عشان الواجهة تستقر
        sendButton?.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        Log.d(TAG, "تم إرسال الرد لـ ${reply.senderName}")
        pendingReply = null
    }

    private fun isCorrectConversation(root: AccessibilityNodeInfo, senderName: String): Boolean {
        // فحص مبسط: هل اسم الشخص ظاهر في شجرة الشاشة الحالية
        return findNodeContainingText(root, senderName) != null
    }

    private fun findEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findEditableNode(child)
            if (result != null) return result
        }
        return null
    }

    private fun findSendButtonNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // ماسنجر بيسمي زرار الإرسال بـ content-description "Send" غالبًا
        if (node.contentDescription?.toString()?.contains("Send", ignoreCase = true) == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findSendButtonNode(child)
            if (result != null) return result
        }
        return null
    }

    private fun findNodeContainingText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeContainingText(child, text)
            if (result != null) return result
        }
        return null
    }

    private fun setTextInNode(node: AccessibilityNodeInfo, text: String) {
        val arguments = Bundle()
        arguments.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text
        )
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }
}
