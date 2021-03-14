package com.jamid.workconnect.interfaces

import com.jamid.workconnect.model.Post

interface RequestItemClickListener {
    fun onRequestItemClick(post: Post)
    fun onPositiveButtonClick(post: Post)
    fun onNegativeButtonClick(post: Post)
    fun onDelete(postId: String, position: Int)
}