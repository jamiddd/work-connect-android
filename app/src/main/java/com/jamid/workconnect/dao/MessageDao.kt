package com.jamid.workconnect.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jamid.workconnect.model.SimpleMessage

@Dao
interface MessageDao {

    @Query("SELECT * FROM simple_message WHERE chatChannelId = :chatChannelId ORDER BY createdAt DESC")
    fun getChatMessages(chatChannelId: String): List<SimpleMessage>

    @Query("SELECT * FROM simple_message WHERE chatChannelId = :chatChannelId ORDER BY createdAt DESC")
    fun getLiveChatMessages(chatChannelId: String): DataSource.Factory<Int, SimpleMessage>

    @Insert(onConflict=OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<SimpleMessage>)

    @Query("SELECT * FROM simple_message WHERE chatChannelId = :chatChannelId AND createdAt < :key ORDER BY createdAt DESC LIMIT 50")
    fun getChatMessagesAfter(chatChannelId: String, key: Long): List<SimpleMessage>

    @Query("DELETE FROM simple_message")
    suspend fun clearMessages()

}