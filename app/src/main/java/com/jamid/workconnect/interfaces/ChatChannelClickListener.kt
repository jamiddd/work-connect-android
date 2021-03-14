package com.jamid.workconnect.interfaces

import com.jamid.workconnect.model.ChatChannel

interface ChatChannelClickListener {
    fun onChatChannelClick(chatChannel: ChatChannel)
}