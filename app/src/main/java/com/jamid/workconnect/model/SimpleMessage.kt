package com.jamid.workconnect.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName="simple_message")
@Parcelize
data class SimpleMessage(
    @PrimaryKey
    var messageId: String,
    var chatChannelId: String,
    var type: String,
    var content: String,
    var senderId: String,
    val createdAt: Long
): Parcelable {
    constructor(): this("", "", "", "","",  0)
}