package com.jamid.workconnect.interfaces

import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.SimpleComment
import com.jamid.workconnect.model.User

interface CommentClickListener {
    suspend fun onCommentLikeClick(comment: SimpleComment): SimpleComment?
    suspend fun onCommentReplyClick(comment: SimpleComment): SimpleComment?
    fun onCommentUserClick(comment: SimpleComment)
    fun onCommentOptionClick(comment: SimpleComment)
    fun onCommentClick(post: Post, comment: SimpleComment)
    suspend fun onFetchUserData(item: SimpleComment): User?
    suspend fun onFetchReplies(item: SimpleComment): List<SimpleComment>
}