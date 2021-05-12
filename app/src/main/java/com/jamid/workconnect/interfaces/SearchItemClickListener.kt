package com.jamid.workconnect.interfaces

interface SearchItemClickListener {
    fun <T> onSearchItemClick(obj: T, clazz: Class<T>)
    fun onSearchAdded(text: String)
}