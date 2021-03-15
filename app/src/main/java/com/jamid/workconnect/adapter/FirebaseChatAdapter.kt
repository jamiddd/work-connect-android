package com.jamid.workconnect.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.request.ImageRequest
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.jamid.workconnect.*
import com.jamid.workconnect.data.ContributorAndChannels
import com.jamid.workconnect.interfaces.MessageItemClickListener
import com.jamid.workconnect.model.SimpleMessage

class FirebaseChatAdapter(
    private val uid: String,
    activity: MainActivity,
    private val users: List<ContributorAndChannels>,
    options: FirestoreRecyclerOptions<SimpleMessage>
): FirestoreRecyclerAdapter<SimpleMessage, FirebaseChatAdapter.FirebaseChatViewHolder>(options){

    private val messageItemClickListener = activity as MessageItemClickListener

    inner class FirebaseChatViewHolder(val view: View, val viewType: Int): RecyclerView.ViewHolder(view) {
        fun bind(simpleMessage: SimpleMessage?) {
            if (simpleMessage != null) {
                /*val context = view.context
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
                    */
                    /*binding.currentUserMessage.setOnClickListener {
                        binding.currentUserMessageTime.visibility = View.VISIBLE
                        TransitionManager.beginDelayedTransition(binding.rightMsgContainer)

                        activity.lifecycleScope.launch {
                            delay(4000)
                            binding.currentUserMessageTime.visibility = View.GONE
                            TransitionManager.beginDelayedTransition(binding.rightMsgContainer)
                        }
                    }*/
                    /*
                } else {
                    val binding = DataBindingUtil.bind<ChatBalloonLeftBinding>(view)!!
                    val user = users.find {
                        it.contributor.id == simpleMessage.senderId
                    }

                    val text = SimpleDateFormat(
                        "hh:mm a",
                        Locale.UK
                    ).format(simpleMessage.createdAt) + " • Sent by ${user?.contributor?.name}"
                    binding.otherUserMessageTime.text = text

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
                    }*/
                    /*binding.otherUserMessage.setOnClickListener {
                        binding.otherUserMessageTime.visibility = View.VISIBLE
                        TransitionManager.beginDelayedTransition(binding.leftMsgContainer)
                        activity.lifecycleScope.launch {
                            delay(4000)
                            binding.otherUserMessageTime.visibility = View.GONE
                            TransitionManager.beginDelayedTransition(binding.leftMsgContainer)
                        }
                    }
                }*/
            }
        }

        private fun initiateImageMessage(
            v: SimpleDraweeView,
            context: Context,
            parent: ConstraintLayout,
            timeText: TextView,
            userPhoto: SimpleDraweeView?,
            message: SimpleMessage,
            right: Boolean = true
        ) {
            val controller = MessageImageControllerListener(v, parent, timeText, userPhoto, context, right)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FirebaseChatViewHolder {
        return when (viewType) {
            OTHER_USER_AT_END, OTHER_USER_AT_START -> {
                FirebaseChatViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_balloon_left, parent, false),
                    viewType
                )
            }
            CURRENT_USER_AT_START, CURRENT_USER_AT_END -> {
                FirebaseChatViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_balloon_right, parent, false),
                    viewType
                )
            }
            else -> FirebaseChatViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.chat_balloon_left, parent, false),
                viewType
            )
        }
    }

    override fun onBindViewHolder(
        holder: FirebaseChatViewHolder,
        position: Int,
        model: SimpleMessage
    ) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        val currentMessage = getItem(position)

        val firstMessageFromBottom = position == 0
        val lastMessageFromBottom = position == itemCount - 1
        val isCurrentUserMessage = currentMessage.senderId == uid
        val isOnlyMessage = itemCount == 1

        when {
            firstMessageFromBottom && !isOnlyMessage -> {
                return if (isCurrentUserMessage)
                    CURRENT_USER_AT_END
                else {
                    OTHER_USER_AT_END
                }
            }
            isOnlyMessage -> {
                return if (isCurrentUserMessage) CURRENT_USER_AT_END else OTHER_USER_AT_END
            }
            lastMessageFromBottom -> {
                val bottomMessage = getItem(position - 1)
                val isSamePreviousSender = bottomMessage.senderId == currentMessage.senderId
                return if (isCurrentUserMessage) {
                    if (isSamePreviousSender) CURRENT_USER_AT_START else CURRENT_USER_AT_END
                } else {
                    OTHER_USER_AT_START
                }
            }
            else -> {
                val topMessage = getItem(position + 1)
                val bottomMessage = getItem(position - 1)
                val isSameBottomSender = bottomMessage.senderId == currentMessage.senderId
                val isSameTopSender = topMessage.senderId == currentMessage.senderId
                return if (isCurrentUserMessage) {
                    when {
                        isSameTopSender && isSameBottomSender -> CURRENT_USER_AT_START
                        isSameTopSender && !isSameBottomSender -> CURRENT_USER_AT_END
                        !isSameTopSender && isSameBottomSender -> CURRENT_USER_AT_START
                        else -> CURRENT_USER_AT_END
                    }
                } else {
                    when {
                        isSameTopSender && isSameBottomSender -> OTHER_USER_AT_START
                        isSameTopSender && !isSameBottomSender -> OTHER_USER_AT_END
                        !isSameTopSender && isSameBottomSender -> OTHER_USER_AT_START
                        else -> OTHER_USER_AT_END
                    }
                }
            }
        }
    }
}