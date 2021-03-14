package com.jamid.workconnect.interfaces

import com.jamid.workconnect.model.Post

interface PostMenuClickListener {
    fun onCollaborateClick(post: Post)
    fun onShareClick(post: Post)
    fun onDeleteClick(post: Post)
    fun onReportClick(post: Post)
}