package com.jamid.workconnect.interfaces

import com.jamid.workconnect.model.Post

interface NotificationClickListener {
    fun onItemClick(post: Post)
}