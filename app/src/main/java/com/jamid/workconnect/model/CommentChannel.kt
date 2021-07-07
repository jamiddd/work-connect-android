package com.jamid.workconnect.model

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "comment_channels")
data class CommentChannel(
    @PrimaryKey
    val commentChannelId: String,
    val parentId: String,
    val postTitle: String,
    val createdAt: Long = System.currentTimeMillis(),
    @Embedded(prefix = "comment_channel_")
    val lastComment: SimpleComment? = null
): Parcelable {
    constructor(): this("", "", "")
}