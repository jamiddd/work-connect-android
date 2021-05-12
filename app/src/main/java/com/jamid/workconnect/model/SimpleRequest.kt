package com.jamid.workconnect.model

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "requests")
@Parcelize
data class SimpleRequest(
    @PrimaryKey
    val id: String,
    val postId: String,
    val receiverId: String,
    val notificationId: String,
    val senderId: String,
    @Embedded(prefix = "user_")
    val sender: User = User(),
    @Embedded(prefix = "post_")
    val post: MicroPost = MicroPost(),
    val createdAt: Long = System.currentTimeMillis()
): Parcelable {
    constructor(): this("", "", "", "", "")
}