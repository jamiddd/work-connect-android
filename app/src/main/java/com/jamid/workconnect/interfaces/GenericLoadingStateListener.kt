package com.jamid.workconnect.interfaces

interface GenericLoadingStateListener {
    fun onInitial()
    fun onLoadingMore()
    fun onLoaded()
    fun onFinished()
    fun onError()
}