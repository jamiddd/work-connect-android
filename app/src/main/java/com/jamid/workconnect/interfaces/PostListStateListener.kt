package com.jamid.workconnect.interfaces

import com.jamid.workconnect.model.Post

interface PostListStateListener {
    fun onListChanged(prevList: List<Post>?, newList: List<Post>?)
}