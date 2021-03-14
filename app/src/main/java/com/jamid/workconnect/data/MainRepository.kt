package com.jamid.workconnect.data

import androidx.lifecycle.LiveData
import com.jamid.workconnect.model.ChatChannel
import com.jamid.workconnect.model.ChatChannelContributor
import com.jamid.workconnect.model.SimpleMessage

class MainRepository(val db: WorkConnectDatabase) {

    private val messageDao = db.messageDao()
    private val messageKeyDao = db.messageKeyDao()
    private val chatChannelContributorDao = db.chatChannelContributorDao()

    fun getChatMessages(chatChannelId: String) : List<SimpleMessage> {
        return messageDao.getChatMessages(chatChannelId)
    }

    suspend fun insertMessages(messages: List<SimpleMessage>) {
        messageDao.insertMessages(messages)
    }

    suspend fun insertKeys(keys: List<MessageKey>) {

    }

    fun getMessageKey(item: SimpleMessage): MessageKey {
        return messageKeyDao.getMessageKey(item.messageId)
    }

    fun getChatMessagesAfter(chatChannelId: String, key: Long): List<SimpleMessage> {
        return messageDao.getChatMessagesAfter(chatChannelId, key)
    }

    suspend fun insertMessage(message: SimpleMessage) {
        messageDao.insertMessages(listOf(message))
    }

    suspend fun insertContributors(contributors: List<ChatChannelContributor>) {
        chatChannelContributorDao.insertContributors(contributors)
        for (contributor in contributors) {
            chatChannelContributorDao.insertContributorChannel(contributor.channelIds)
        }
    }

    fun channelContributors(chatChannel: ChatChannel): LiveData<List<ContributorAndChannels>> {
        return chatChannelContributorDao.getChatChannelContributors(chatChannel.chatChannelId)
    }

    suspend fun clearChatChannelContributorsAndChannelIds() {
        chatChannelContributorDao.clearEverything()
    }
}