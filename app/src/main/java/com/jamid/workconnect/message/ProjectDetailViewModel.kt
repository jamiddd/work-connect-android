package com.jamid.workconnect.message

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.POSTS
import com.jamid.workconnect.model.ChatChannel
import com.jamid.workconnect.model.ChatChannelContributor
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.Result

class ProjectDetailViewModel: ViewModel() {

    private val db: FirebaseFirestore = Firebase.firestore

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

    private val _guidelinesUpdateResult = MutableLiveData<Result<String>>()
    val guidelinesUpdateResult: LiveData<Result<String>> = _guidelinesUpdateResult

    fun updateGuidelines(guidelines: String) {
        val map = mapOf("guidelines" to guidelines)

        val post = currentPost.value
        if (post != null) {
            db.collection(POSTS).document(post.id)
                .update(map)
                .addOnSuccessListener {
                    _guidelinesUpdateResult.postValue(Result.Success(guidelines))
                }.addOnFailureListener {
                    _guidelinesUpdateResult.postValue(Result.Error(it))
                }
        } else {
            _guidelinesUpdateResult.postValue(Result.Error(Exception("Post is null.")))
        }
    }


    fun setGuidelinesUpdateResult(result: Result<String>?) {
        _guidelinesUpdateResult.postValue(result)
    }

    fun postGuidelinesUpdate(data: String, onComplete: () -> Unit) {
        _guidelinesUpdateResult.postValue(null)
        val post = currentPost.value
        post?.guidelines = data
        _currentPost.postValue(post)
        onComplete()
    }

}