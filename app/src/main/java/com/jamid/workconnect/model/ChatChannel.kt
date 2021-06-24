package com.jamid.workconnect.model

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "chat_channels")
data class ChatChannel(
    @PrimaryKey
    var chatChannelId: String,
    var postId: String,
    var postTitle: String,
    var postImage: String? = null,
    var contributorsCount: Long = 0,
    var administrators: List<String> = emptyList(),
    var contributorsList: List<String> = emptyList(),
    var registrationTokens: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    @Embedded(prefix = "message_")
    var lastMessage: SimpleMessage? = null
): Parcelable {
    constructor(): this("", "", "")
}