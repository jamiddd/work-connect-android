package com.jamid.workconnect.interfaces

import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.SimpleComment

interface PostItemClickListener {

    fun onItemClick(post: Post, viewHolder: Any? = null)
    fun onUserPressed(post: Post)
    fun onOptionClick(post: Post)


    // the state before changes
    suspend fun onLikePressed(post: Post): Post

    // the state before changes
    suspend fun onDislikePressed(post: Post): Post

    // the state before changes
    suspend fun onSavePressed(post: Post): Post

    // the state before changes
    suspend fun onFollowPressed(post: Post): Post

    //
    fun onNotSignedIn(post: Post)

    fun onCommentClick(post: Post)

    suspend fun onFetchComments(post: Post): List<SimpleComment>

}