package com.jamid.workconnect.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.firebase.ui.firestore.paging.LoadingState
import com.firebase.ui.firestore.paging.LoadingState.*
import com.jamid.workconnect.DOCUMENT
import com.jamid.workconnect.IMAGE
import com.jamid.workconnect.R
import com.jamid.workconnect.databinding.ChatChannelLayoutBinding
import com.jamid.workconnect.interfaces.ChatChannelClickListener
import com.jamid.workconnect.interfaces.GenericLoadingStateListener
import com.jamid.workconnect.model.ChatChannel
import java.text.SimpleDateFormat
import java.util.*

class ChatChannelAdapter(
    options: FirestorePagingOptions<ChatChannel>,
    private val genericLoadingStateListener: GenericLoadingStateListener,
    private val chatChannelClickListener: ChatChannelClickListener
) : FirestorePagingAdapter<ChatChannel, ChatChannelAdapter.ChatChannelViewHolder>(options) {

    inner class ChatChannelViewHolder(val binding: ChatChannelLayoutBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(chatChannel: ChatChannel?) {
            if (chatChannel != null) {
                if (chatChannel.postImage != null) {
                    binding.chatChannelImg.setImageURI(chatChannel.postImage)
                }

                binding.chatChannelName.text = chatChannel.postTitle
                binding.chatChannelLastUpdated.text = SimpleDateFormat("hh:mm a", Locale.UK).format(chatChannel.updatedAt)

                binding.root.setOnClickListener {
                    chatChannelClickListener.onChatChannelClick(chatChannel)
                }

                val lastMessage = chatChannel.lastMessage
                if (lastMessage != null) {
                    binding.chatChannelLastMsgContent.visibility = View.VISIBLE
                    val lastMsgText = lastMessage.content
                    when (lastMessage.type) {
                        IMAGE -> {
                            binding.chatChannelLastMsgContent.text = IMAGE
                            binding.chatChannelLastMsgContent.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC)
                        }
                        DOCUMENT -> {
                            binding.chatChannelLastMsgContent.text = DOCUMENT
                            binding.chatChannelLastMsgContent.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC)
                        }
                        else -> {
                            binding.chatChannelLastMsgContent.text = lastMsgText
                        }
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatChannelViewHolder {
        val binding = DataBindingUtil.inflate<ChatChannelLayoutBinding>(LayoutInflater.from(parent.context), R.layout.chat_channel_layout, parent, false)
        return ChatChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatChannelViewHolder, position: Int, model: ChatChannel) {
        holder.bind(getItem(position)?.toObject(model::class.java))
    }

    override fun onLoadingStateChanged(state: LoadingState) {
        when (state) {
            LOADING_INITIAL -> genericLoadingStateListener.onInitial()
            LOADING_MORE -> genericLoadingStateListener.onLoadingMore()
            LOADED -> genericLoadingStateListener.onLoaded()
            FINISHED -> genericLoadingStateListener.onFinished()
            ERROR -> genericLoadingStateListener.onError()
        }
    }
}