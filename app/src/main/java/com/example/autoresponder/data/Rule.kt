package com.example.autoresponder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReplyType {
    FIXED_TEXT,   // نص ثابت
    AI_SMART      // رد ذكي عبر Claude API
}

@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    // اسم الشخص المستهدف، أو "*" يعني الكل (قاعدة عامة)
    val senderName: String,
    val replyType: ReplyType = ReplyType.FIXED_TEXT,
    val fixedReplyText: String = "",
    // بيتفعل بس في الأوقات دي (24h format)، لو فاضي يبقى شغال طول الوقت
    val activeFromHour: Int = 0,
    val activeToHour: Int = 24,
    val isEnabled: Boolean = true
)
