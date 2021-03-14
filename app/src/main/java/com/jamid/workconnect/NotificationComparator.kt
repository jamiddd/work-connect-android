package com.jamid.workconnect

import androidx.recyclerview.widget.DiffUtil
import com.jamid.workconnect.model.SimpleNotification

class NotificationComparator : DiffUtil.ItemCallback<SimpleNotification>() {
    override fun areItemsTheSame(
        oldItem: SimpleNotification,
        newItem: SimpleNotification
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: SimpleNotification,
        newItem: SimpleNotification
    ): Boolean {
        return oldItem.content == newItem.content
    }

}