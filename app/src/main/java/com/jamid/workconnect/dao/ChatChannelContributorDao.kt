package com.jamid.workconnect.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.jamid.workconnect.data.ChannelIds
import com.jamid.workconnect.data.ContributorAndChannels
import com.jamid.workconnect.model.ChatChannelContributor

@Dao
interface ChatChannelContributorDao {

    @Insert(onConflict=OnConflictStrategy.REPLACE)
    suspend fun insertContributors(contributors: List<ChatChannelContributor>)

    @Transaction
    @Query("SELECT * FROM chat_user LEFT JOIN channel_ids ON chat_user.id = channel_ids.userId WHERE chatChannelId = :chatChannelId")
    fun getChatChannelContributors(chatChannelId: String): LiveData<List<ContributorAndChannels>>

    @Query("SELECT * FROM chat_user WHERE id = :userId LIMIT 1")
    suspend fun getContributor(userId: String): ChatChannelContributor?

    @Insert(onConflict=OnConflictStrategy.REPLACE)
    fun insertContributorChannel(channels: List<ChannelIds>)


    @Query("DELETE FROM chat_user")
    fun deleteAllChatUsers()

    @Query("DELETE FROM channel_ids")
    fun deleteAllChannelIds()

    @Transaction
    suspend fun clearEverything() {
        deleteAllChatUsers()
        deleteAllChannelIds()
    }

}