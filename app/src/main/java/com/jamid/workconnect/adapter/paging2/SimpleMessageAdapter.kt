package com.jamid.workconnect.adapter.paging2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.model.SimpleMessage

class SimpleMessageAdapter(
    private val viewModel: MainViewModel,
    private val activity: MainActivity
) : PagedListAdapter<SimpleMessage, SimpleMessageViewHolder>(GenericComparator(SimpleMessage::class.java)) {

    val uid = Firebase.auth.currentUser?.uid ?: ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleMessageViewHolder {
        return when (viewType) {
            OTHER_USER_AT_END, OTHER_USER_AT_MIDDLE, OTHER_USER_AT_START -> {
                SimpleMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_balloon_left, parent, false),
                    viewType,
                    activity,
                    viewModel
                )
            }
            OTHER_USER_AT_START_IMAGE, OTHER_USER_AT_END_IMAGE -> {
                SimpleMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_balloon_image_left, parent, false),
                    viewType,
                    activity,
                    viewModel
                )
            }
            OTHER_USER_AT_START_DOC, OTHER_USER_AT_END_DOC -> {
                SimpleMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_balloon_doc_left, parent, false),
                    viewType,
                    activity,
                    viewModel
                )
            }
            CURRENT_USER_AT_START, CURRENT_USER_AT_MIDDLE, CURRENT_USER_AT_END -> {
                SimpleMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_balloon_right, parent, false),
                    viewType,
                    activity,
                    viewModel
                )
            }
            CURRENT_USER_AT_END_IMAGE, CURRENT_USER_AT_START_IMAGE -> {
                SimpleMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_balloon_image_right, parent, false),
                    viewType,
                    activity,
                    viewModel
                )
            }
            CURRENT_USER_AT_START_DOC, CURRENT_USER_AT_END_DOC -> {
                SimpleMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.chat_balloon_doc_right, parent, false),
                    viewType,
                    activity,
                    viewModel
                )
            }
            else -> SimpleMessageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.chat_balloon_left, parent, false),
                viewType,
                activity,
                viewModel
            )
        }
    }

    override fun onBindViewHolder(holder: SimpleMessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)!!

        val firstMessageFromBottom = position == 0
        val lastMessageFromBottom = position == itemCount - 1
        val isCurrentUserMessage = message.senderId == uid
        val isOnlyMessage = itemCount == 1

        when {
            firstMessageFromBottom && !isOnlyMessage -> {
                return if (isCurrentUserMessage)
                    when (message.type) {
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
                    when (message.type) {
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
                    when (message.type) {
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
                    when (message.type) {
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
                val isSamePreviousSender = bottomMessage.senderId == message.senderId
                return if (isCurrentUserMessage) {
                    if (isSamePreviousSender) {
                        when (message.type) {
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
                        when (message.type) {
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
                    if (isSamePreviousSender) {
                        when (message.type) {
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
                    } else {
                        when (message.type) {
                            IMAGE -> {
                                OTHER_USER_AT_START_IMAGE
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
            else -> {
                val topMessage = getItem(position + 1)!!
                val bottomMessage = getItem(position - 1)!!
                val isSameBottomSender = bottomMessage.senderId == message.senderId
                val isSameTopSender = topMessage.senderId == message.senderId
                if (isCurrentUserMessage) {
                    return when {
                        isSameTopSender && isSameBottomSender -> {
                            when (message.type) {
                                IMAGE -> {
                                    CURRENT_USER_AT_START_IMAGE
                                }
                                DOCUMENT -> {
                                    CURRENT_USER_AT_START_DOC
                                }
                                else -> {
                                    CURRENT_USER_AT_MIDDLE
                                }
                            }
                        }
                        isSameTopSender && !isSameBottomSender -> {
                            when (message.type) {
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
                            when (message.type) {
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
                        !isSameTopSender && !isSameBottomSender -> {
                            when (message.type) {
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
                        else -> throw Exception("Illegal State exception.")
                    }
                } else {
                    return when {
                        isSameTopSender && isSameBottomSender -> {
                            when (message.type) {
                                IMAGE -> {
                                    OTHER_USER_AT_START_IMAGE
                                }
                                DOCUMENT -> {
                                    OTHER_USER_AT_START_DOC
                                }
                                else -> {
                                    OTHER_USER_AT_MIDDLE
                                }
                            }
                        }
                        isSameTopSender && !isSameBottomSender -> {
                            when (message.type) {
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
                            when (message.type) {
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
                        !isSameTopSender && !isSameBottomSender -> {
                            when (message.type) {
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
                        else -> throw IllegalStateException("Shouldn't happen though.")
                    }
                }
            }
        }
    }
}

