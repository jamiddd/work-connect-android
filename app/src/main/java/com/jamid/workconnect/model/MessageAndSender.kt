package com.jamid.workconnect.model

import androidx.room.Embedded
import androidx.room.Relation

data class MessageAndSender(
    @Embedded
    val message: SimpleMessage,
    @Relation(parentColumn = "senderId", entityColumn = "id")
    val sender: ChatChannelContributor
)