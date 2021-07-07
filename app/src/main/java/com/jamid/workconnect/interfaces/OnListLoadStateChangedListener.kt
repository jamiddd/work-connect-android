package com.jamid.workconnect.interfaces


interface OnListLoadStateChangedListener {
    fun onListEmpty()
    fun onListNonEmpty()
    fun onLoadError()
}