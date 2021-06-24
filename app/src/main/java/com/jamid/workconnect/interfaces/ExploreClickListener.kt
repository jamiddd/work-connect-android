package com.jamid.workconnect.interfaces

interface ExploreClickListener {
    fun <T: Any> onSeeMoreClick(clazz: Class<T>, postType: String? = null)
}