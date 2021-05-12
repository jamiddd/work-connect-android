package com.jamid.workconnect.adapter.paging2

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.jamid.workconnect.MainActivity
import com.jamid.workconnect.R
import com.jamid.workconnect.model.ChatChannel

// deprecated
class ChannelAdapter(private val uid: String, private val activity: MainActivity): ListAdapter<ChatChannel, ChatChannelViewHolder>(
    GenericComparator(ChatChannel::class.java)
) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChatChannelViewHolder {
        return ChatChannelViewHolder.newInstance(parent, R.layout.chat_channel_layout)
    }

    override fun onBindViewHolder(holder: ChatChannelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}