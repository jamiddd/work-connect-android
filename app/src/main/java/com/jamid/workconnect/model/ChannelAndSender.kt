package com.jamid.workconnect.model

import androidx.room.Embedded
import androidx.room.Relation

data class ChannelAndSender(
    @Embedded
    val channel: ChatChannel?,
    @Relation(parentColumn = "message_senderId", entityColumn = "id")
    val lastSender: ChatChannelContributor?
)