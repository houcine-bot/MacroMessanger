package com.example.autoresponder.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * عميل بسيط لاستدعاء Claude API عشان يولد رد ذكي بناءً على رسالة المرسل.
 * لازم المستخدم يدخل مفتاح API الخاص بيه من إعدادات التطبيق.
 */
object ClaudeApiClient {
    private val client = OkHttpClient()
    private const val ENDPOINT = "https://api.anthropic.com/v1/messages"

    suspend fun generateReply(apiKey: String, senderName: String, incomingMessage: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("model", "claude-sonnet-4-6")
                    put("max_tokens", 200)
                    put("messages", JSONArray().put(
                        JSONObject().apply {
                            put("role", "user")
                            put(
                                "content",
                                "رد بشكل مهذب ومختصر (سطر أو اتنين) نيابة عني على رسالة ماسنجر دي من \"$senderName\": \"$incomingMessage\""
                            )
                        }
                    ))
                }

                val request = Request.Builder()
                    .url(ENDPOINT)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("content-type", "application/json")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext ""
                    val json = JSONObject(response.body?.string() ?: return@withContext "")
                    val content = json.optJSONArray("content") ?: return@withContext ""
                    if (content.length() == 0) return@withContext ""
                    content.getJSONObject(0).optString("text", "")
                }
            } catch (e: IOException) {
                ""
            }
        }
    }
}
