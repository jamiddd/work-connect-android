package com.jamid.workconnect.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jamid.workconnect.data.MessageKey

@Dao
interface MessageKeyDao {

    @Insert(onConflict=OnConflictStrategy.REPLACE)
    suspend fun insertMessageKeys(keys: List<MessageKey>)

    @Query("SELECT * FROM message_key WHERE current = :messageId LIMIT 1")
    fun getMessageKey(messageId: String) : MessageKey
}