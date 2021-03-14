package com.jamid.workconnect

import androidx.recyclerview.widget.DiffUtil
import com.jamid.workconnect.model.SimpleMessage

class MessageComparator() : DiffUtil.ItemCallback<SimpleMessage>() {

    override fun areItemsTheSame(oldItem: SimpleMessage, newItem: SimpleMessage): Boolean {
        return oldItem.messageId == newItem.messageId
    }

    override fun areContentsTheSame(oldItem: SimpleMessage, newItem: SimpleMessage): Boolean {
        return oldItem == newItem
    }

}