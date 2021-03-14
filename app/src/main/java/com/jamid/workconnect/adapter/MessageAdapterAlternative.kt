package com.jamid.workconnect.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.IMAGE
import com.jamid.workconnect.MessageComparator
import com.jamid.workconnect.R
import com.jamid.workconnect.TEXT
import com.jamid.workconnect.databinding.ChatBalloonLeftBinding
import com.jamid.workconnect.databinding.ChatBalloonRightBinding
import com.jamid.workconnect.interfaces.GenericLoadingStateListener
import com.jamid.workconnect.interfaces.MessageItemClickListener
import com.jamid.workconnect.model.ChatChannelContributor
import com.jamid.workconnect.model.SimpleMessage
import kotlinx.coroutines.CoroutineScope

class MessageAdapterAlternative(
    private val users: List<ChatChannelContributor>,
    private val scope: CoroutineScope,
    private val genericLoadingStateListener: GenericLoadingStateListener,
    private val messageItemClickListener: MessageItemClickListener,
    private val lifecycleOwner: LifecycleOwner
): ListAdapter<SimpleMessage, MessageViewHolderAlternative>(MessageComparator()) {

    private val uid: String = Firebase.auth.currentUser?.uid.toString()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MessageViewHolderAlternative {
        return when (viewType) {
            3, 1 -> {
                MessageViewHolderAlternative(LayoutInflater.from(parent.context).inflate(R.layout.chat_balloon_left, parent, false), viewType, users)
            }
            0, 2 -> {
                MessageViewHolderAlternative(LayoutInflater.from(parent.context).inflate(R.layout.chat_balloon_right, parent, false), viewType, users)
            }
            else -> throw Exception("No viewType provided!")
        }
    }

    override fun onBindViewHolder(holder: MessageViewHolderAlternative, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        val currentMessage = getItem(position)
//        if (currentMessage.type == "Placeholder") return 4
        val firstMessageFromBottom = position == 0
        val lastMessageFromBottom = position == itemCount - 1
        val isCurrentUserMessage = currentMessage.senderId == uid
        val isOnlyMessage = itemCount == 1

        return when {
            firstMessageFromBottom && !isOnlyMessage -> {
                if (isCurrentUserMessage) {
                    2
                } else {
                    val bottomMessage = getItem(position - 1)
                    val isSameBottomSender = bottomMessage.senderId == currentMessage.senderId
                    if (isSameBottomSender) {
                        1
                    } else {
                        3
                    }
                }
            }
            isOnlyMessage -> {
                if (isCurrentUserMessage) 2 else 3
            }
            lastMessageFromBottom -> {
                val topMessage = getItem(position - 1)
                val isSamePreviousSender = topMessage.senderId == currentMessage.senderId
                return if (isCurrentUserMessage) {
                    if (isSamePreviousSender) 0 else 2
                } else {
                    3
                }
            }
            else -> {
                val bottomMessage = getItem(position + 1)
                val topMessage = getItem(position - 1)
                val isSameBottomSender = bottomMessage.senderId == currentMessage.senderId
                val isSameTopSender = topMessage.senderId == currentMessage.senderId
                if (isCurrentUserMessage) {
                    return when {
                        isSameTopSender && isSameBottomSender -> {
                            0
                        }
                        isSameTopSender && !isSameBottomSender -> {
                            2
                        }
                        !isSameTopSender && isSameBottomSender -> {
                            0
                        }
                        else -> {
                            2
                        }
                    }
                } else {
                    return when{
                        isSameTopSender && isSameBottomSender -> {
                            1
                        }
                        isSameTopSender && !isSameBottomSender -> {
                            3
                        }
                        !isSameTopSender && isSameBottomSender -> {
                            1
                        }
                        else -> {
                            3
                        }
                    }
                }
            }
        }
    }

}

class LoadingViewHolder(val view: View): RecyclerView.ViewHolder(view) {
    fun bind() {
        val seeMoreBtn = view.findViewById<Button>(R.id.button)
        val seeMoreProgress = view.findViewById<ProgressBar>(R.id.seeMoreProgress)
        seeMoreBtn.setOnClickListener {
            seeMoreBtn.visibility = View.INVISIBLE
            seeMoreProgress.visibility = View.VISIBLE

            /*val user = viewModel.user.value
            if (user != null) {
                val list = mutableListOf<String>()
                if (user.savedPosts.size <= end) {
                    savedPostsAdapter.submitList(user.savedPosts)
                } else {
                    list.addAll(user.savedPosts.subList(0, end))
                    list.add(DUMMY)
                    savedPostsAdapter.submitList(list)
                }
            }*/
        }
    }
}

class MessageViewHolderAlternative(val view: View, val viewType: Int, val users: List<ChatChannelContributor>): RecyclerView.ViewHolder(view) {

    private val uid = Firebase.auth.currentUser?.uid.toString()

    fun bind(message: SimpleMessage) {
        val isCurrentUserMessage = message.senderId == uid
        if (isCurrentUserMessage) {
            val binding = DataBindingUtil.bind<ChatBalloonRightBinding>(view)!!
            when {
                viewType == 0 && message.type == IMAGE -> {
                    binding.imgMsgRight.visibility = View.VISIBLE
                    binding.currentUserMessage.visibility = View.GONE
                    binding.imgMsgRight.setImageURI(message.content)
                }
                viewType == 0 && message.type == TEXT -> {
                    binding.imgMsgRight.visibility = View.GONE
                    binding.currentUserMessage.visibility = View.VISIBLE
                    binding.currentUserMessage.text = message.content
                }
                viewType == 2 && message.type == IMAGE -> {
                    binding.imgMsgRight.visibility = View.VISIBLE
                    binding.currentUserMessage.visibility = View.GONE
                    binding.imgMsgRight.setImageURI(message.content)
                }
                viewType == 2 && message.type == TEXT -> {
                    binding.imgMsgRight.visibility = View.GONE
                    binding.currentUserMessage.visibility = View.VISIBLE
                    binding.currentUserMessage.text = message.content
                }
                else -> Toast.makeText(
                    binding.root.context,
                    "Something went wrong",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            val binding = DataBindingUtil.bind<ChatBalloonLeftBinding>(view)!!
            val user = users.find {
                it.id == message.senderId
            }!!

            when {
                viewType == 1 && message.type == IMAGE -> {
                    binding.imgMsgLeft.visibility = View.VISIBLE
                    binding.otherUserMessage.visibility = View.GONE
                    binding.imgMsgLeft.setImageURI(message.content)
                }
                viewType == 1 && message.type == TEXT -> {
                    binding.imgMsgLeft.visibility = View.GONE
                    binding.otherUserMessage.visibility = View.VISIBLE
                    binding.otherUserMessage.text = message.content

                }
                viewType == 3 && message.type == IMAGE -> {
                    binding.imgMsgLeft.visibility = View.VISIBLE
                    binding.otherUserMessage.visibility = View.GONE
                    binding.imgMsgLeft.setImageURI(message.content)
                }
                viewType == 3 && message.type == TEXT -> {
                    binding.imgMsgLeft.visibility = View.GONE
                    binding.otherUserMessage.visibility = View.VISIBLE
                    binding.otherUserMessage.text = message.content
                }
                else -> Toast.makeText(
                    binding.root.context,
                    "Something went wrong",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}