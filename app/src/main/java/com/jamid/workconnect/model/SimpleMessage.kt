package com.jamid.workconnect.model

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Entity(tableName="simple_message")
@Parcelize
data class SimpleMessage(
    @PrimaryKey
    var messageId: String,
    var chatChannelId: String,
    var type: String,
    var content: String,
    var senderId: String,
    @Embedded(prefix = "meta_")
    var metaData: MediaMetaData? = null,
    @Embedded(prefix = "sender_")
    @Exclude @set: Exclude @get: Exclude
    var sender: User = User(),
    val createdAt: Long = System.currentTimeMillis(),
    @Exclude @set: Exclude @get: Exclude
    var isDownloaded: Boolean = false
): Parcelable {
    constructor(): this("", "", "", "", "")

    fun isEmpty() = messageId.isBlank() || messageId.isEmpty()

    fun isNotEmpty() = messageId.isNotEmpty() || messageId.isNotBlank()

}