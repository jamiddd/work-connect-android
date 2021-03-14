package com.jamid.workconnect.data

import androidx.paging.ItemKeyedDataSource
import com.jamid.workconnect.model.SimpleMessage

class MessageDataSource(val chatChannelId: String, private val repository: MainRepository) : ItemKeyedDataSource<Long, SimpleMessage>() {
    override fun getKey(item: SimpleMessage): Long {
        return item.createdAt
    }

    override fun loadInitial(
        params: LoadInitialParams<Long>,
        callback: LoadInitialCallback<SimpleMessage>
    ) {
        val items = repository.getChatMessages(chatChannelId)
        callback.onResult(items)
    }

    override fun loadAfter(params: LoadParams<Long>, callback: LoadCallback<SimpleMessage>) {
        val items = repository.getChatMessagesAfter(chatChannelId, params.key)
        callback.onResult(items)
    }

    override fun loadBefore(params: LoadParams<Long>, callback: LoadCallback<SimpleMessage>) {
        //
    }

}