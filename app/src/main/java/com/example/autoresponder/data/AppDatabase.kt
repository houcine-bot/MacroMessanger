package com.example.autoresponder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.SharedPreferences

@Database(entities = [Rule::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autoresponder_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/** تخزين بسيط لمفتاح Claude API وحالة تفعيل الخدمة */
object AppSettings {
    private const val PREFS = "autoresponder_prefs"
    private const val KEY_API = "claude_api_key"
    private const val KEY_ENABLED = "service_enabled"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun setApiKey(context: Context, key: String) {
        prefs(context).edit().putString(KEY_API, key).apply()
    }

    fun getApiKey(context: Context): String =
        prefs(context).getString(KEY_API, "") ?: ""

    fun setServiceEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isServiceEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, true)
}
