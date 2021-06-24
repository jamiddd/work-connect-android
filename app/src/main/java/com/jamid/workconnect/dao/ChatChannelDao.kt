package com.jamid.workconnect.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import com.jamid.workconnect.model.ChannelAndSender
import com.jamid.workconnect.model.ChatChannel

@Dao
abstract class ChatChannelDao: BaseDao<ChatChannel>() {

    @Transaction
    @Query("SELECT * FROM chat_channels ORDER BY updatedAt DESC")
    abstract fun getChatChannels(): LiveData<List<ChannelAndSender>>

    @Query("SELECT * FROM chat_channels LIMIT 1")
    abstract suspend fun checkIfChatChannelsEmpty(): List<ChatChannel>?

    @Query("SELECT * FROM chat_channels ORDER BY updatedAt DESC")
    abstract fun allChannels(): LiveData<List<ChatChannel>>

    @Query("SELECT * FROM chat_channels ORDER BY updatedAt DESC")
    abstract fun chatChannels(): DataSource.Factory<Int, ChatChannel>

    @Query("SELECT * FROM chat_channels WHERE chatChannelId = :chatChannelId")
    abstract suspend fun getChatChannel(chatChannelId: String): ChatChannel?

    @Query("SELECT * FROM chat_channels WHERE message_sender_id = :id")
    abstract fun getChatChannelsForLastMessage(id: String): List<ChatChannel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertChatChannels(channels: List<ChatChannel>)

    @Query("DELETE FROM chat_channels")
    abstract suspend fun clearChatChannels()

    @Query("SELECT * FROM chat_channels WHERE postTitle LIKE :query ORDER BY updatedAt DESC")
    abstract suspend fun getChannelsForQuery(query: String): List<ChatChannel>

}