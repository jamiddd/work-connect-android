package com.jamid.workconnect.model

import android.os.Parcelable
import androidx.room.Entity
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize
@Entity(tableName="chat_user", primaryKeys = ["id", "channelId"])
@Parcelize
@IgnoreExtraProperties
data class ChatChannelContributor(
    var id: String,
    var name: String,
    var username: String,
    var photo: String?,
    var admin: Boolean,
    @Exclude @set: Exclude @get: Exclude
    var channelId: String = ""
): Parcelable {
    constructor(): this("", "", "", null, true)
}