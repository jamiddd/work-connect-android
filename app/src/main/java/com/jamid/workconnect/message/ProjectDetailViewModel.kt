package com.jamid.workconnect.message

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jamid.workconnect.model.ChatChannel
import com.jamid.workconnect.model.ChatChannelContributor
import com.jamid.workconnect.model.Post

class ProjectDetailViewModel: ViewModel() {

    private val _currentChatChannel = MutableLiveData<ChatChannel>().apply {
        value = null
    }
    val currentChatChannel: LiveData<ChatChannel> = _currentChatChannel

    private val _currentContributor = MutableLiveData<ChatChannelContributor>().apply {
        value = null
    }
    val currentContributor: LiveData<ChatChannelContributor> = _currentContributor

    private val _currentPost = MutableLiveData<Post>().apply {
        value = null
    }
    val currentPost: LiveData<Post> = _currentPost

    fun setCurrentChatChannel(chatChannel: ChatChannel?) {
        _currentChatChannel.postValue(chatChannel)
    }

    fun setCurrentContributor(chatChannelContributor: ChatChannelContributor?) {
        _currentContributor.postValue(chatChannelContributor)
    }

    fun setCurrentPost(post: Post?) {
        _currentPost.postValue(post)
    }

}