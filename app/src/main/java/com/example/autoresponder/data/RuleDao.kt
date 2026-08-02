package com.example.autoresponder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY id DESC")
    fun getAllRules(): Flow<List<Rule>>

    @Query("SELECT * FROM rules WHERE senderName = :name AND isEnabled = 1 LIMIT 1")
    suspend fun getRuleForSender(name: String): Rule?

    @Query("SELECT * FROM rules WHERE senderName = '*' AND isEnabled = 1 LIMIT 1")
    suspend fun getDefaultRule(): Rule?

    @Insert
    suspend fun insert(rule: Rule)

    @Update
    suspend fun update(rule: Rule)

    @Delete
    suspend fun delete(rule: Rule)
}
