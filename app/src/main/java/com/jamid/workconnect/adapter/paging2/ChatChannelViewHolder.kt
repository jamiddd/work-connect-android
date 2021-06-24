package com.jamid.workconnect.adapter.paging2

import android.os.Build
import android.text.SpannableString
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import com.facebook.drawee.view.SimpleDraweeView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.DOCUMENT
import com.jamid.workconnect.IMAGE
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.GenericViewHolder
import com.jamid.workconnect.interfaces.ChatChannelClickListener
import com.jamid.workconnect.model.ChatChannel
import java.text.SimpleDateFormat
import java.util.*

class ChatChannelViewHolder(parent: ViewGroup, @LayoutRes layout: Int): GenericViewHolder<ChatChannel>(parent, layout) {

    private val chatChannelClickListener = parent.context as ChatChannelClickListener
    private val uid = Firebase.auth.currentUser?.uid ?: ""

    override fun bind(item: ChatChannel) {

        val postImage = itemView.findViewById<SimpleDraweeView>(R.id.chatChannelImg)
        val lastUpdated = itemView.findViewById<TextView>(R.id.chatChannelLastUpdated)
        val name = itemView.findViewById<TextView>(R.id.chatChannelName)
        val messageContent = itemView.findViewById<TextView>(R.id.chatChannelLastMsgContent)

        if (item.postImage != null) {
            postImage.setImageURI(item.postImage)
        }

        if (Build.VERSION.SDK_INT <= 27) {
            name.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
            messageContent.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
        }

        name.text = item.postTitle
        lastUpdated.text =
            SimpleDateFormat("hh:mm a", Locale.UK).format(item.updatedAt)

        itemView.setOnClickListener {
            chatChannelClickListener.onChatChannelClick(item)
        }

        val lastMessage = item.lastMessage
        if (lastMessage != null) {
            messageContent.visibility = View.VISIBLE
            val lastSender = lastMessage.sender

            val senderName = if (lastMessage.senderId == uid) {
                "You"
            } else {
                lastSender.name
            }

            val lastMsgText = lastMessage.content
            when (lastMessage.type) {
                IMAGE -> {
                    val spannableString = SpannableString("$senderName: $IMAGE")
                    messageContent.text = spannableString
                }
                DOCUMENT -> {
                    val spannableString = SpannableString("$senderName: $DOCUMENT")
                    messageContent.text = spannableString
                }
                else -> {
                    val spannableString = SpannableString("$senderName: $lastMsgText")
                    messageContent.text = spannableString
                }
            }
            // Residual Code
//              spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, name.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

        }
    }

    companion object {

        @JvmStatic
        fun newInstance(parent: ViewGroup, @LayoutRes layout: Int) = ChatChannelViewHolder(parent, layout)
    }
}