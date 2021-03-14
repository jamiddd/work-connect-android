package com.jamid.workconnect.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.request.ImageRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.IMAGE
import com.jamid.workconnect.MessageComparator
import com.jamid.workconnect.R
import com.jamid.workconnect.data.ContributorAndChannels
import com.jamid.workconnect.databinding.ChatBalloonLeftBinding
import com.jamid.workconnect.databinding.ChatBalloonRightBinding
import com.jamid.workconnect.interfaces.GenericLoadingStateListener
import com.jamid.workconnect.interfaces.MessageItemClickListener
import com.jamid.workconnect.model.SimpleMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val OTHER_USER_AT_START = 0
private const val OTHER_USER_AT_END = 2
private const val CURRENT_USER_AT_START = 1
private const val CURRENT_USER_AT_END = 3

class SimpleMessageAdapter(
    private val users: List<ContributorAndChannels>,
    private val messageItemClickListener: MessageItemClickListener,
    private val scope: CoroutineScope,
    private val genericLoadingStateListener: GenericLoadingStateListener
) : PagedListAdapter<SimpleMessage, SimpleMessageViewHolder>(MessageComparator()) {

    val uid = Firebase.auth.currentUser?.uid ?: ""

    override fun onCurrentListChanged(
        previousList: PagedList<SimpleMessage>?,
        currentList: PagedList<SimpleMessage>?
    ) {
        super.onCurrentListChanged(previousList, currentList)
        genericLoadingStateListener.onLoadingMore()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleMessageViewHolder {
        return when (viewType) {
            OTHER_USER_AT_END, OTHER_USER_AT_START -> {
                SimpleMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_balloon_left, parent, false),
                    viewType,
                    messageItemClickListener,
                    scope,
                    users
                )
            }
            CURRENT_USER_AT_START, CURRENT_USER_AT_END -> {
                SimpleMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_balloon_right, parent, false),
                    viewType,
                    messageItemClickListener,
                    scope,
                    users
                )
            }
            else -> SimpleMessageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.chat_balloon_left, parent, false),
                viewType,
                messageItemClickListener,
                scope,
                users
            )
        }
    }

    override fun onBindViewHolder(holder: SimpleMessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        val currentMessage = getItem(position)!!

        val firstMessageFromBottom = position == 0
        val lastMessageFromBottom = position == itemCount - 1
        val isCurrentUserMessage = currentMessage.senderId == uid
        val isOnlyMessage = itemCount == 1

        when {
            firstMessageFromBottom && !isOnlyMessage -> {
                return if (isCurrentUserMessage)
                    CURRENT_USER_AT_END
                else {
                    /*val topMessage = getItem(position + 1)!!
                    val isSameTopSender = topMessage.senderId == currentMessage.senderId
                    if (isSameTopSender) {

                    } else {
                        OTHER_USER_AT_START
                    }*/
                    OTHER_USER_AT_END
                }
            }
            isOnlyMessage -> {
                return if (isCurrentUserMessage) CURRENT_USER_AT_END else OTHER_USER_AT_END
            }
            lastMessageFromBottom -> {
                val bottomMessage = getItem(position - 1)!!
                val isSamePreviousSender = bottomMessage.senderId == currentMessage.senderId
                return if (isCurrentUserMessage) {
                    if (isSamePreviousSender) CURRENT_USER_AT_START else CURRENT_USER_AT_END
                } else {
                    OTHER_USER_AT_START
                }
            }
            else -> {
                val topMessage = getItem(position + 1)!!
                val bottomMessage = getItem(position - 1)!!
                val isSameBottomSender = bottomMessage.senderId == currentMessage.senderId
                val isSameTopSender = topMessage.senderId == currentMessage.senderId
                if (isCurrentUserMessage) {
                    return when {
                        isSameTopSender && isSameBottomSender -> {
                            if (position == 6) {
                                Log.d("SimpleMessageAdapter", "6 " + getItem(position - 2)!!.content)
                            }
                            CURRENT_USER_AT_START
                        }
                        isSameTopSender && !isSameBottomSender -> {
                            if (position == 6) {
                                Log.d("SimpleMessageAdapter", "6")
                            }
                            CURRENT_USER_AT_END
                        }
                        !isSameTopSender && isSameBottomSender -> {
                            CURRENT_USER_AT_START
                        }
                        else -> {
                            if (position == 6) {
                                Log.d("SimpleMessageAdapter", "6")
                            }
                            CURRENT_USER_AT_END
                        }
                    }
                } else {
                    return when {
                        isSameTopSender && isSameBottomSender -> {
                            OTHER_USER_AT_START
                        }
                        isSameTopSender && !isSameBottomSender -> {
                            OTHER_USER_AT_END
                        }
                        !isSameTopSender && isSameBottomSender -> {
                            OTHER_USER_AT_START
                        }
                        else -> {
                            OTHER_USER_AT_END
                        }
                    }
                }
            }
        }
    }
}

class SimpleMessageViewHolder(
    val view: View,
    val viewType: Int,
    private val messageItemClickListener: MessageItemClickListener,
    private val scope: CoroutineScope,
    private val users: List<ContributorAndChannels>
) : RecyclerView.ViewHolder(view) {

    val uid = Firebase.auth.currentUser?.uid ?: " "

    @SuppressLint("SetTextI18n")
    fun bind(simpleMessage: SimpleMessage?) {
        if (simpleMessage != null) {
            val context = view.context
            val isCurrentUserMessage = simpleMessage.senderId == uid
            if (isCurrentUserMessage) {
                val binding = DataBindingUtil.bind<ChatBalloonRightBinding>(view)!!

                binding.currentUserMessageTime.text = SimpleDateFormat(
                    "hh:mm a",
                    Locale.UK
                ).format(simpleMessage.createdAt)

                when (viewType) {
                    CURRENT_USER_AT_START -> {
                        binding.rightMsgTail.visibility = View.GONE
                        when (simpleMessage.type) {
                            IMAGE -> {
                                binding.imgMsgRight.visibility = View.VISIBLE
                                binding.currentUserMessage.visibility = View.GONE
                                initiateImageMessage(binding.imgMsgRight, context, simpleMessage)
                            }
                            else -> {
                                binding.currentUserMessage.text = simpleMessage.content
                                binding.currentUserMessage.visibility = View.VISIBLE
                            }
                        }
                    }
                    CURRENT_USER_AT_END -> {
                        if (simpleMessage.type == IMAGE) {
                            binding.imgMsgRight.visibility = View.VISIBLE
                            binding.rightMsgContainer.visibility = View.GONE
                            initiateImageMessage(binding.imgMsgRight, context, simpleMessage)
                        } else {
                            binding.currentUserMessage.text = simpleMessage.content
                            binding.rightMsgContainer.visibility = View.VISIBLE
                            binding.imgMsgRight.visibility = View.GONE
                            binding.rightMsgTail.visibility = View.VISIBLE
                        }
                    }
                }
                binding.currentUserMessage.setOnClickListener {
                    binding.currentUserMessageTime.visibility = View.VISIBLE
                    TransitionManager.beginDelayedTransition(binding.rightMsgContainer)
                    scope.launch {
                        delay(4000)
                        binding.currentUserMessageTime.visibility = View.GONE
                        TransitionManager.beginDelayedTransition(binding.rightMsgContainer)
                    }
                }
            } else {
                val binding = DataBindingUtil.bind<ChatBalloonLeftBinding>(view)!!
                val user = users.find {
                    it.contributor.id == simpleMessage.senderId
                }
                Log.d("SIMP_ADA", users.toString())
                binding.otherUserMessageTime.text = SimpleDateFormat(
                    "hh:mm a",
                    Locale.UK
                ).format(simpleMessage.createdAt) + " • Sent by ${user?.contributor?.name}"
                when (viewType) {
                    OTHER_USER_AT_START -> {
                        binding.otherUserPhoto.visibility = View.INVISIBLE
                        if (simpleMessage.type == IMAGE) {
                            binding.imgMsgLeft.visibility = View.VISIBLE
                            binding.leftTextContent.visibility = View.GONE
                            initiateImageMessage(binding.imgMsgLeft, context, simpleMessage, false)
                        } else {
                            binding.otherUserMessage.text = simpleMessage.content
                            binding.leftTextContent.visibility = View.VISIBLE
                            binding.leftMsgTail.visibility = View.GONE
                            binding.imgMsgLeft.visibility = View.GONE
                        }
                    }
                    OTHER_USER_AT_END -> {
                        binding.otherUserPhoto.setImageURI(user?.contributor?.photo)
                        binding.otherUserPhoto.visibility = View.VISIBLE
                        if (simpleMessage.type == IMAGE) {
                            binding.leftTextContent.visibility = View.GONE
                            binding.imgMsgLeft.visibility = View.VISIBLE
                            initiateImageMessage(binding.imgMsgLeft, context, simpleMessage, false)
                        } else {
                            binding.leftTextContent.visibility = View.VISIBLE
                            binding.leftMsgTail.visibility = View.VISIBLE
                            binding.imgMsgLeft.visibility = View.GONE
                            binding.otherUserMessage.text = simpleMessage.content
                        }
                    }
                }


                binding.otherUserMessage.setOnClickListener {
                    binding.otherUserMessageTime.visibility = View.VISIBLE
                    TransitionManager.beginDelayedTransition(binding.leftMsgContainer)
                    scope.launch {
                        delay(4000)
                        binding.otherUserMessageTime.visibility = View.GONE
                        TransitionManager.beginDelayedTransition(binding.leftMsgContainer)
                    }
                }
            }
        }
    }

    private fun initiateImageMessage(
        v: SimpleDraweeView,
        context: Context,
        message: SimpleMessage,
        right: Boolean = true
    ) {
        val controller = MessageImageControllerListener(v, context, right)
        val imgRequest = ImageRequest.fromUri(message.content)

        val imgController = Fresco.newDraweeControllerBuilder()
            .setImageRequest(imgRequest)
            .setControllerListener(controller)
            .build()

        v.controller = imgController

        v.setOnClickListener {
            if (controller.measuredWidth != 0) {
                messageItemClickListener.onImageClick(v, controller.measuredWidth, controller.measuredHeight, message)
            }
        }
    }
}