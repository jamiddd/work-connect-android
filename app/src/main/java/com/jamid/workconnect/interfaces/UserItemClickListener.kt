package com.jamid.workconnect.interfaces

import com.jamid.workconnect.model.User

interface UserItemClickListener {
    fun onUserPressed(userId: String)
    fun onUserPressed(user: User)
    fun onFollowPressed(otherUser: User)
}