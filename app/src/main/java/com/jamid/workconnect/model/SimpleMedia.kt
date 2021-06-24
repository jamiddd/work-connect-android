package com.jamid.workconnect.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName="simple_media")
data class SimpleMedia(
    @PrimaryKey var id: String,
    val type: String,
    var mediaLocation: String,
    val createdAt: Long,
    var chatChannelId: String,
    val senderId: String,
    var originalFileName: String? = null,
    var size: Long = 0
) : Parcelable { constructor(): this("", "", "", 0, "", "")}