package com.jamid.workconnect.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SimpleNotification(
    val id: String,
    val content: String,
    val postId: String,
    val userId: String,
    val senderId: String,
    val chatChannelId: String?,
    val requestId: String?,
    val createdAt: Long
): Parcelable {
    constructor() : this("", "", "", "", "", null, null, 0)
}