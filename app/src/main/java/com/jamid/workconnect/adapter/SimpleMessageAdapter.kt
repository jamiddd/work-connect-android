package com.jamid.workconnect.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.request.ImageRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.jamid.workconnect.*
import com.jamid.workconnect.data.ContributorAndChannels
import com.jamid.workconnect.interfaces.MessageItemClickListener
import com.jamid.workconnect.model.SimpleMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SimpleMessageAdapter(
    private val users: List<ContributorAndChannels>,
    private val scope: CoroutineScope,
    private val activity: MainActivity
) : PagedListAdapter<SimpleMessage, SimpleMessageViewHolder>(MessageComparator()) {

    val uid = Firebase.auth.currentUser?.uid ?: ""
    private val messageItemClickListener = activity as MessageItemClickListener

    override fun onCurrentListChanged(
        previousList: PagedList<SimpleMessage>?,
        currentList: PagedList<SimpleMessage>?
    ) {
        super.onCurrentListChanged(previousList, currentList)
        currentList?.forEach {
            Log.d("SimpleMessage", it.toString())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleMessageViewHolder {
        return when (viewType) {
            OTHER_USER_AT_END, OTHER_USER_AT_START -> {
                SimpleMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_balloon_left, parent, false),
                    viewType,
                    activity,
                    scope,
                    users
                )
            }
            OTHER_USER_AT_START_IMAGE, OTHER_USER_AT_END_IMAGE -> {
                SimpleMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_balloon_image_left, parent, false),
                    viewType,
                    activity,
                    scope,
                    users
                )
            }
            OTHER_USER_AT_START_DOC, OTHER_USER_AT_END_DOC -> {
                SimpleMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_balloon_doc_left, parent, false),
                    viewType,
                    activity,
                    scope,
                    users
                )
            }
            CURRENT_USER_AT_START, CURRENT_USER_AT_END -> {
                SimpleMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_balloon_right, parent, false),
                    viewType,
                    activity,
                    scope,
                    users
                )
            }
            CURRENT_USER_AT_END_IMAGE, CURRENT_USER_AT_START_IMAGE -> {
                SimpleMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_balloon_image_right, parent, false),
                    viewType,
                    activity,
                    scope,
                    users
                )
            }
            CURRENT_USER_AT_START_DOC, CURRENT_USER_AT_END_DOC -> {
                SimpleMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_balloon_doc_right, parent, false),
                    viewType,
                    activity,
                    scope,
                    users
                )
            }
            else -> SimpleMessageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.chat_balloon_left, parent, false),
                viewType,
                activity,
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
                    when (currentMessage.type) {
                        IMAGE -> {
                            CURRENT_USER_AT_END_IMAGE
                        }
                        DOCUMENT -> {
                            CURRENT_USER_AT_END_DOC
                        }
                        else -> {
                            CURRENT_USER_AT_END
                        }
                    }
                else {
                    when (currentMessage.type) {
                        IMAGE -> {
                            OTHER_USER_AT_END_IMAGE
                        }
                        DOCUMENT -> {
                            OTHER_USER_AT_END_DOC
                        }
                        else -> {
                            OTHER_USER_AT_END
                        }
                    }
                }
            }
            isOnlyMessage -> {
                return if (isCurrentUserMessage) {
                    when (currentMessage.type) {
                        IMAGE -> {
                            CURRENT_USER_AT_END_IMAGE
                        }
                        DOCUMENT -> {
                            CURRENT_USER_AT_END_DOC
                        }
                        else -> {
                            CURRENT_USER_AT_END
                        }
                    }
                } else {
                    when (currentMessage.type) {
                        IMAGE -> {
                            OTHER_USER_AT_END_IMAGE
                        }
                        DOCUMENT -> {
                            OTHER_USER_AT_END_DOC
                        }
                        else -> {
                            OTHER_USER_AT_END
                        }
                    }
                }
            }
            lastMessageFromBottom -> {
                val bottomMessage = getItem(position - 1)!!
                val isSamePreviousSender = bottomMessage.senderId == currentMessage.senderId
                return if (isCurrentUserMessage) {
                    if (isSamePreviousSender) {
                        when (currentMessage.type) {
                            IMAGE -> {
                                CURRENT_USER_AT_START_IMAGE
                            }
                            DOCUMENT -> {
                                CURRENT_USER_AT_START_DOC
                            }
                            else -> {
                                CURRENT_USER_AT_START
                            }
                        }
                    } else {
                        when (currentMessage.type) {
                            IMAGE -> {
                                CURRENT_USER_AT_END_IMAGE
                            }
                            DOCUMENT -> {
                                CURRENT_USER_AT_END_DOC
                            }
                            else -> {
                                CURRENT_USER_AT_END
                            }
                        }
                    }
                } else {
                    when (currentMessage.type) {
                        IMAGE -> {
                            OTHER_USER_AT_START_IMAGE
                        }
                        DOCUMENT -> {
                            OTHER_USER_AT_START_DOC
                        }
                        else -> {
                            OTHER_USER_AT_START
                        }
                    }
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
                            when (currentMessage.type) {
                                IMAGE -> {
                                    CURRENT_USER_AT_START_IMAGE
                                }
                                DOCUMENT -> {
                                    CURRENT_USER_AT_START_DOC
                                }
                                else -> {
                                    CURRENT_USER_AT_START
                                }
                            }
                        }
                        isSameTopSender && !isSameBottomSender -> {
                            when (currentMessage.type) {
                                IMAGE -> {
                                    CURRENT_USER_AT_END_IMAGE
                                }
                                DOCUMENT -> {
                                    CURRENT_USER_AT_END_DOC
                                }
                                else -> {
                                    CURRENT_USER_AT_END
                                }
                            }
                        }
                        !isSameTopSender && isSameBottomSender -> {
                            when (currentMessage.type) {
                                IMAGE -> {
                                    CURRENT_USER_AT_START_IMAGE
                                }
                                DOCUMENT -> {
                                    CURRENT_USER_AT_START_DOC
                                }
                                else -> {
                                    CURRENT_USER_AT_START
                                }
                            }
                        }
                        else -> {
                            when (currentMessage.type) {
                                IMAGE -> {
                                    CURRENT_USER_AT_END_IMAGE
                                }
                                DOCUMENT -> {
                                    CURRENT_USER_AT_END_DOC
                                }
                                else -> {
                                    CURRENT_USER_AT_END
                                }
                            }
                        }
                    }
                } else {
                    return when {
                        isSameTopSender && isSameBottomSender -> {
                            when (currentMessage.type) {
                                IMAGE -> {
                                    OTHER_USER_AT_START_IMAGE
                                }
                                DOCUMENT -> {
                                    OTHER_USER_AT_START_DOC
                                }
                                else -> {
                                    OTHER_USER_AT_START
                                }
                            }
                        }
                        isSameTopSender && !isSameBottomSender -> {
                            when (currentMessage.type) {
                                IMAGE -> {
                                    OTHER_USER_AT_END_IMAGE
                                }
                                DOCUMENT -> {
                                    OTHER_USER_AT_END_DOC
                                }
                                else -> {
                                    OTHER_USER_AT_END
                                }
                            }
                        }
                        !isSameTopSender && isSameBottomSender -> {
                            when (currentMessage.type) {
                                IMAGE -> {
                                    OTHER_USER_AT_START_IMAGE
                                }
                                DOCUMENT -> {
                                    OTHER_USER_AT_START_DOC
                                }
                                else -> {
                                    OTHER_USER_AT_START
                                }
                            }
                        }
                        else -> {
                            when (currentMessage.type) {
                                IMAGE -> {
                                    OTHER_USER_AT_END_IMAGE
                                }
                                DOCUMENT -> {
                                    OTHER_USER_AT_END_DOC
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
    }
}

class SimpleMessageViewHolder(
    val view: View,
    val viewType: Int,
    val activity: MainActivity,
    private val scope: CoroutineScope,
    private val users: List<ContributorAndChannels>
) : RecyclerView.ViewHolder(view) {

    private val messageItemClickListener = activity as MessageItemClickListener

    val uid = Firebase.auth.currentUser?.uid ?: " "

    @SuppressLint("SetTextI18n")
    fun bind(simpleMessage: SimpleMessage?) {
        if (simpleMessage != null) {
            val context = view.context
            val isCurrentUserMessage = simpleMessage.senderId == uid
            if (isCurrentUserMessage) {
                val parent1 = view.findViewById<ConstraintLayout>(R.id.chatRightParent)
                val parent2 = view.findViewById<ConstraintLayout>(R.id.chatImgRightParent)

                val time1 = view.findViewById<TextView>(R.id.currentUserMessageTime)
                val time2 = view.findViewById<TextView>(R.id.currentUserImgMsgTime)

                time1?.text = SimpleDateFormat(
                    "hh:mm a",
                    Locale.UK
                ).format(simpleMessage.createdAt)

                time2?.text = SimpleDateFormat(
                    "hh:mm a",
                    Locale.UK
                ).format(simpleMessage.createdAt)

                val tail1 = view.findViewById<ImageView>(R.id.rightMsgTail)
                val tail2 = view.findViewById<ImageView>(R.id.rightImgMsgTail)
                val imgMsg = view.findViewById<SimpleDraweeView>(R.id.currentUserImgMsg)
                val textMsg = view.findViewById<TextView>(R.id.currentUserMessage)
                val docParent1 = view.findViewById<ConstraintLayout>(R.id.chatDocRightParent)

                when (viewType) {
                    CURRENT_USER_AT_START -> {
                        tail1?.visibility = View.GONE
                        textMsg.text = simpleMessage.content
                        textMsg.visibility = View.VISIBLE
                    }
                    CURRENT_USER_AT_START_IMAGE -> {
                        imgMsg.visibility = View.VISIBLE
                        initiateImageMessage(imgMsg, context, parent2, time2, null, simpleMessage)
                    }
                    CURRENT_USER_AT_START_DOC -> {
                        setupDoc(simpleMessage, docParent1)
                    }
                    CURRENT_USER_AT_END -> {
                        textMsg.text = simpleMessage.content
                        tail1.visibility = View.VISIBLE
                    }
                    CURRENT_USER_AT_END_IMAGE -> {
                        imgMsg.visibility = View.VISIBLE
                        tail2.visibility = View.GONE
                        initiateImageMessage(imgMsg, context, parent2, time2, null, simpleMessage)
                    }
                    CURRENT_USER_AT_END_DOC -> {
                        setupDoc(simpleMessage, docParent1)
                    }
                }

                textMsg?.setOnClickListener {
                    time1?.visibility = View.VISIBLE
                    scope.launch {
                        delay(4000)
                        time1?.visibility = View.GONE
                    }
                }

            } else {

                val parent1 = view.findViewById<ConstraintLayout>(R.id.chatLeftParent)
                val parent2 = view.findViewById<ConstraintLayout>(R.id.chatImageLeftParent)

                val time1 = view.findViewById<TextView>(R.id.otherUserMessageTime)
                val time2 = view.findViewById<TextView>(R.id.otherUserImgMessageTime)

                val tail1 = view.findViewById<ImageView>(R.id.leftMsgTail)
                val tail2 = view.findViewById<ImageView>(R.id.leftImgMsgTail)
                val imgMsg = view.findViewById<SimpleDraweeView>(R.id.otherUserImageMessage)
                val textMsg = view.findViewById<TextView>(R.id.otherUserMessage)
                val otherUserT = view.findViewById<SimpleDraweeView>(R.id.otherUserPhoto)
                val otherUserI = view.findViewById<SimpleDraweeView>(R.id.otherUserImgMsgPhoto)

                val docName2 = view.findViewById<TextView>(R.id.otherUserDocName)
                val docParent2 = view.findViewById<ConstraintLayout>(R.id.chatDocLeftParent)

                val user = users.find {
                    it.contributor.id == simpleMessage.senderId
                }

                time1?.text = SimpleDateFormat(
                    "hh:mm a",
                    Locale.UK
                ).format(simpleMessage.createdAt) + " • Sent by ${user?.contributor?.name}"

                time2?.text = SimpleDateFormat(
                    "hh:mm a",
                    Locale.UK
                ).format(simpleMessage.createdAt) + " • Sent by ${user?.contributor?.name}"

                when (viewType) {
                    OTHER_USER_AT_START -> {
                        otherUserT.visibility = View.INVISIBLE
                        textMsg.text = simpleMessage.content
                        tail1.visibility = View.GONE
                    }
                    OTHER_USER_AT_START_IMAGE -> {
                        otherUserI.visibility = View.INVISIBLE
                        imgMsg.visibility = View.VISIBLE
                        initiateImageMessage(imgMsg, context, parent2, time2, otherUserI, simpleMessage, false)
                    }
                    OTHER_USER_AT_START_DOC -> {
                        setupDoc(simpleMessage, docParent2)
                    }
                    OTHER_USER_AT_END -> {
                        otherUserT.setImageURI(user?.contributor?.photo)
                        otherUserT.visibility = View.VISIBLE
                        tail1.visibility = View.VISIBLE
                        textMsg.text = simpleMessage.content
                    }
                    OTHER_USER_AT_END_IMAGE -> {
                        otherUserI.setImageURI(user?.contributor?.photo)
                        otherUserI.visibility = View.VISIBLE
                        tail2.visibility = View.VISIBLE
                        imgMsg.visibility = View.VISIBLE
                        initiateImageMessage(imgMsg, context, parent2, time2, otherUserI, simpleMessage, false)
                    }
                    OTHER_USER_AT_END_DOC -> {
                        setupDoc(simpleMessage, docParent2)
                    }
                }

                textMsg?.setOnClickListener {
                    time1?.visibility = View.VISIBLE
                    scope.launch {
                        delay(4000)
                        time1?.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun setupDoc(message: SimpleMessage, view: ViewGroup) {

        val currentUserDocName = view.findViewById<TextView>(R.id.currentUserDocName)
        val currentUserDocSize = view.findViewById<TextView>(R.id.currentUserDocSize)
        val otherUserDocName = view.findViewById<TextView>(R.id.otherUserDocName)
        val otherUserDocSize = view.findViewById<TextView>(R.id.otherUserDocSize)
        val words = message.content.split("%2F")
        val fullName = words.last().split('?')[0]
        val name = fullName.split('_').last()
        currentUserDocName?.text = name
        otherUserDocName?.text = name

        val childRef = "${message.chatChannelId}/documents/messages/$fullName"
        val objectRef = Firebase.storage.reference.child(childRef)

        objectRef.metadata.addOnSuccessListener {

            val sizeText = (it.sizeBytes / 1024).toString() + " KB"
            otherUserDocSize?.text = sizeText
            currentUserDocSize?.text = sizeText

            view.setOnClickListener { v ->
                messageItemClickListener.onDocumentClick(message, fullName, it.sizeBytes)
            }

        }.addOnFailureListener {
            Log.d("SimpleAdapter", it.localizedMessage.toString())
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
                messageItemClickListener.onImageClick(v, /*controller.measuredWidth, controller.measuredHeight,*/ message)
            }
        }
    }
}