package com.jamid.workconnect.dao

import androidx.paging.DataSource
import androidx.room.*
import com.jamid.workconnect.model.SimpleMessage

@Dao
abstract class MessageDao : BaseDao<SimpleMessage>() {

    @Query("SELECT * FROM simple_message WHERE chatChannelId = :chatChannelId ORDER BY createdAt DESC")
    abstract fun getChatMessages(chatChannelId: String): List<SimpleMessage>

    @Transaction
    @Query("SELECT * FROM simple_message WHERE chatChannelId = :chatChannelId ORDER BY createdAt DESC")
    abstract fun getLiveChatMessages(chatChannelId: String): DataSource.Factory<Int, SimpleMessage>

    @Insert(onConflict=OnConflictStrategy.REPLACE)
    abstract suspend fun insertMessages(messages: List<SimpleMessage>)

    @Query("SELECT * FROM simple_message WHERE chatChannelId = :chatChannelId AND createdAt < :key ORDER BY createdAt DESC LIMIT 50")
    abstract fun getChatMessagesAfter(chatChannelId: String, key: Long): List<SimpleMessage>

    @Query("DELETE FROM simple_message")
    abstract suspend fun clearMessages()

    @Query("SELECT * FROM simple_message WHERE chatChannelId = :channelId ORDER BY createdAt DESC LIMIT 1")
    abstract suspend fun getLastMessage(channelId: String): SimpleMessage?

    @Query("SELECT * FROM simple_message WHERE chatChannelId = :chatChannelId AND type = :type AND isDownloaded = 1 ORDER BY createdAt DESC")
    abstract fun getImageMessages(chatChannelId: String, type: String): DataSource.Factory<Int, SimpleMessage>
}