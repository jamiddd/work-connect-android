package com.jamid.workconnect.interfaces

import com.jamid.workconnect.model.Post

interface NotificationClickListener {
    fun onNotificationItemClick(post: Post)
    fun onNotificationItemClick(postId: String)
    fun <T> onNotificationPositiveClicked(obj: T, clazz: Class<T>)
    fun <T> onNotificationNegativeClicked(obj: T, clazz: Class<T>)
}