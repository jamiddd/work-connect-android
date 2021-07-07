package com.jamid.workconnect.model

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Entity(tableName = "simple_comment")
@Parcelize
data class SimpleComment(
    @PrimaryKey
    val commentId: String,
    var commentContent: String,
    var likes: Long,
    val senderId: String,
    var postId: String,
    var commentChannelId: String,
    var threadChannelId: String,
    var repliesCount: Long = 0,
    var commentLevel: Long = 0,
    var postedAt: Long = System.currentTimeMillis(),
    @Exclude @set: Exclude @get: Exclude
    @Embedded(prefix = "comment_")
    var sender: User = User(),
    @Exclude @set: Exclude @get: Exclude
    var isLiked: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    var postTitle: String = "",
): Parcelable {
    constructor(): this("", "", 0, "", "", "", "")
}