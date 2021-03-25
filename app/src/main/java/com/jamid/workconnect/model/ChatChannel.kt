package com.jamid.workconnect.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChatChannel(
    var chatChannelId: String,
    var postId: String,
    var postTitle: String,
    var postImage: String?,
    val contributorsList: List<String>,
    val registrationTokens: List<String>,
    val createdAt: Long,
    var updatedAt: Long,
    var lastMessage: SimpleMessage?
): Parcelable {
    constructor(): this("", "", "", "", emptyList(), emptyList(), 0, 0, null)
}