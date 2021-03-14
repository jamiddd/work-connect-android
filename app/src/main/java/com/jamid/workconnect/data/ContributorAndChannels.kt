package com.jamid.workconnect.data

import androidx.room.Embedded
import androidx.room.Relation
import com.jamid.workconnect.model.ChatChannelContributor

data class ContributorAndChannels(
    @Embedded
    val contributor: ChatChannelContributor,
    @Relation(parentColumn="id", entityColumn="userId")
    val chatChannelIds: List<ChannelIds>
)