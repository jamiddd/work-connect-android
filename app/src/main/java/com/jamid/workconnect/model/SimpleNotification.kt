package com.jamid.workconnect.model

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "notifications")
data class SimpleNotification(
    @PrimaryKey
    var id: String,
    var receiverId: String,
    var type: String,
    var requestId: String? = null,
    @Embedded(prefix="user_")
    var sender: User = User(),
    var createdAt: Long = System.currentTimeMillis(),
    @Embedded(prefix = "post_")
    var post: MicroPost? = null
): Parcelable {
    constructor() : this("", "", "")
}