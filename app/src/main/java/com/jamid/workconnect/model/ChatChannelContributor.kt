package com.jamid.workconnect.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.jamid.workconnect.data.ChannelIds
import kotlinx.parcelize.Parcelize

@Entity(tableName="chat_user", primaryKeys=["id"])
@Parcelize
@IgnoreExtraProperties
data class ChatChannelContributor(
    var id: String,
    var name: String,
    var username: String,
    var photo: String?,
    var admin: Boolean,
    @Ignore
    @Exclude @set: Exclude @get: Exclude
    var channelIds: List<ChannelIds> = emptyList()
): Parcelable {
    constructor(): this("", "", "", null, true)
}