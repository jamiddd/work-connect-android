package com.jamid.workconnect.adapter

import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.firebase.ui.firestore.paging.LoadingState
import com.firebase.ui.firestore.paging.LoadingState.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.ChatChannelLayoutBinding
import com.jamid.workconnect.interfaces.ChatChannelClickListener
import com.jamid.workconnect.interfaces.GenericLoadingStateListener
import com.jamid.workconnect.model.ChatChannel
import com.jamid.workconnect.model.UserMinimal
import java.text.SimpleDateFormat
import java.util.*

class ChatChannelAdapter(
    options: FirestorePagingOptions<ChatChannel>,
    activity: MainActivity
) : FirestorePagingAdapter<ChatChannel, ChatChannelAdapter.ChatChannelViewHolder>(options) {

    private val chatChannelClickListener = activity as ChatChannelClickListener
    private val genericLoadingStateListener = activity as GenericLoadingStateListener

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

                    Firebase.firestore.collection(USER_MINIMALS).document(lastMessage.senderId)
                        .get()
                        .addOnSuccessListener {
                            if (it != null && it.exists()) {
                                val user = it.toObject(UserMinimal::class.java)
                                if (user != null) {
                                    val name = if (user.id == Firebase.auth.currentUser?.uid) {
                                        "You"
                                    } else {
                                        user.name
                                    }
                                    val lastMsgText = lastMessage.content
                                    when (lastMessage.type) {
                                        IMAGE -> {
                                            val spannableString = SpannableString("$name: $IMAGE")
//                                            spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, name.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                                            binding.chatChannelLastMsgContent.text = spannableString
                                        }
                                        DOCUMENT -> {
                                            val spannableString = SpannableString("$name: $DOCUMENT")
//                                            spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, name.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                                            binding.chatChannelLastMsgContent.text = spannableString
                                        }
                                        else -> {
                                            val spannableString = SpannableString("$name: $lastMsgText")
//                                            spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, name.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                                            binding.chatChannelLastMsgContent.text = spannableString
                                        }
                                    }
                                }
                            }

                        }.addOnFailureListener {

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