package com.jamid.workconnect.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import com.jamid.workconnect.model.ChatChannelContributor

@Dao
interface ChatChannelContributorDao {

    @Insert(onConflict=OnConflictStrategy.REPLACE)
    suspend fun insertContributors(contributors: List<ChatChannelContributor>)

    @Query("SELECT * FROM chat_user WHERE channelId = :chatChannelId")
    fun getChatChannelContributors(chatChannelId: String): LiveData<List<ChatChannelContributor>>

    @Query("SELECT * FROM chat_user WHERE channelId = :chatChannelId")
    fun getPagedContributors(chatChannelId: String): DataSource.Factory<Int, ChatChannelContributor>

    @Query("SELECT * FROM chat_user WHERE id = :userId AND channelId = :chatChannelId LIMIT 1")
    suspend fun getContributor(userId: String, chatChannelId: String): ChatChannelContributor?

    /*@Insert(onConflict=OnConflictStrategy.REPLACE)
    fun insertContributorChannel(channels: List<ChannelIds>)
*/

    @Query("DELETE FROM chat_user")
    fun deleteAllChatUsers()
/*
    @Query("DELETE FROM channel_ids")
    fun deleteAllChannelIds()*/

    @Transaction
    suspend fun clearEverything() {
        deleteAllChatUsers()
//        deleteAllChannelIds()
    }

}