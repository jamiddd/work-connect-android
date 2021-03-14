package com.jamid.workconnect.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName="channel_ids")
@Parcelize
data class ChannelIds(
    @PrimaryKey(autoGenerate=true)
    var someId: Int = 0,
    var chatChannelId: String,
    var userId: String
): Parcelable