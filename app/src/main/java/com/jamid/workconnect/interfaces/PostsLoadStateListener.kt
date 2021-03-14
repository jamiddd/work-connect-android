package com.jamid.workconnect.interfaces

interface PostsLoadStateListener {
    fun onInitial()
    fun onLoadingMore()
    fun onLoaded()
    fun onFinished()
    fun onError()
}