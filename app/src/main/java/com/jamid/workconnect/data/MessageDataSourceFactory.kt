package com.jamid.workconnect.data

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import com.jamid.workconnect.model.SimpleMessage

class MessageDataSourceFactory(val chatChannelId: String, val repository: MainRepository) : DataSource.Factory<Long, SimpleMessage>() {

    val sourceLiveData = MutableLiveData<MessageDataSource>()
    lateinit var latestSource: MessageDataSource

    override fun create(): DataSource<Long, SimpleMessage> {
        latestSource = MessageDataSource(chatChannelId, repository)
        sourceLiveData.postValue(latestSource)
        return latestSource
    }
}