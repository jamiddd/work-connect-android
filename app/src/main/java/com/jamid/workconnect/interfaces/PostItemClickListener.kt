package com.jamid.workconnect.interfaces

import com.jamid.workconnect.adapter.paging3.PostViewHolder
import com.jamid.workconnect.model.Post

interface PostItemClickListener {
    fun onItemClick(post: Post, viewHolder: PostViewHolder? = null)
    fun onUserPressed(post: Post)
    fun onOptionClick(post: Post)


    // the state before changes
    fun onLikePressed(post: Post): Post

    // the state before changes
    fun onDislikePressed(post: Post): Post

    // the state before changes
    fun onSavePressed(post: Post): Post

    // the state before changes
    fun onFollowPressed(post: Post): Post

    //
    fun onNotSignedIn(post: Post)

}