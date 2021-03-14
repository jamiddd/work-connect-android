package com.jamid.workconnect.interfaces

import com.jamid.workconnect.model.Post

interface PostItemClickListener {
    fun onItemClick(post: Post)
    fun onLikePressed(post: Post, prevL: Boolean, prevD: Boolean)
    fun onDislikePressed(post: Post, prevL: Boolean, prevD: Boolean)
    fun onSavePressed(post: Post, prev: Boolean)
    fun onFollowPressed(post: Post, prev: Boolean)
    fun onUserPressed(post: Post)
    fun onOptionClick(post: Post)
}