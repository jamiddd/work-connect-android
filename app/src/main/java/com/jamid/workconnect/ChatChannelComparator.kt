package com.jamid.workconnect

import androidx.recyclerview.widget.DiffUtil
import com.google.firebase.firestore.DocumentSnapshot
import com.jamid.workconnect.model.ChatChannel

class ChatChannelComparator : DiffUtil.ItemCallback<DocumentSnapshot>() {
    override fun areItemsTheSame(oldItem: DocumentSnapshot, newItem: DocumentSnapshot): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: DocumentSnapshot, newItem: DocumentSnapshot): Boolean {
        val chatChannel1 = oldItem.toObject(ChatChannel::class.java)!!
        val chatChannel2 = newItem.toObject(ChatChannel::class.java)!!
        return chatChannel1.chatChannelId == chatChannel2.chatChannelId
    }

}