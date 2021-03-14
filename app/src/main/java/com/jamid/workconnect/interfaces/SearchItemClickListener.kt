package com.jamid.workconnect.interfaces

interface SearchItemClickListener {
    fun onSearchItemClick(id: String, type: String?)
    fun onSearchAdded(text: String)
}