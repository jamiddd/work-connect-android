package com.jamid.workconnect.data

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.CHAT_CHANNELS
import com.jamid.workconnect.CREATED_AT
import com.jamid.workconnect.MESSAGES
import com.jamid.workconnect.model.MessageAndSender
import com.jamid.workconnect.model.SimpleMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MessageBoundaryCallback(private val chatChannelId: String, private val repository: MainRepository, private val scope: CoroutineScope) : PagedList.BoundaryCallback<MessageAndSender>() {

    private val db = Firebase.firestore
    private val networkErrors = MutableLiveData<Exception>()
    private var lastDoc: DocumentSnapshot? = null
    private var firstDoc: DocumentSnapshot? = null
    private var hasReachedEnd = false

    companion object {

        const val PAGE_SIZE = 50
        const val TAG = "MessageBoundary"

        fun newInstance(chatChannelId: String, repository: MainRepository, scope: CoroutineScope)
            = MessageBoundaryCallback(chatChannelId, repository, scope)
    }

    override fun onZeroItemsLoaded() {
        super.onZeroItemsLoaded()

        Log.d(TAG, "OnZeroItemsLoaded - $chatChannelId")

        db.collection(CHAT_CHANNELS).document(chatChannelId)
            .collection(MESSAGES)
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener {
                val messages = it.toObjects(SimpleMessage::class.java)
                val keys = mutableListOf<MessageKey>()
                for (i in 0 until messages.size) {
                    val prev = if (i == 0) {
                        null
                    } else {
                        messages[i - 1].messageId
                    }
                    val current = messages[i].messageId
                    val next = if (i == messages.size - 1) {
                        null
                    } else {
                        messages[i + 1].messageId
                    }

                    keys.add(MessageKey(prev, current, next))
                }

                scope.launch (Dispatchers.IO) {
                    repository.updateMessage(messages)
//                    repository.insertKeys(keys)
                }

                if (!it.isEmpty) {
                    firstDoc = it.first()
                    lastDoc = it.last()
                }
            }.addOnFailureListener {
                networkErrors.postValue(it)
            }

    }

    override fun onItemAtFrontLoaded(itemAtFront: MessageAndSender) {
        super.onItemAtFrontLoaded(itemAtFront)

        if (firstDoc == null) {
            if (!hasReachedEnd) {
                Log.d("OnItemAtFrontLoaded", chatChannelId)
                db.collection(CHAT_CHANNELS).document(chatChannelId).collection(MESSAGES).document(itemAtFront.message.messageId)
                    .get()
                    .addOnSuccessListener { doc ->
                        if (doc != null && doc.exists()) {
                            getMessagesBefore(doc)
                        }
                    }.addOnFailureListener {
                        networkErrors.postValue(it)
                    }
            }
        } else {
            getMessagesBefore(firstDoc!!)
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: MessageAndSender) {
        super.onItemAtEndLoaded(itemAtEnd)

        if (lastDoc == null) {
            if (!hasReachedEnd) {
                db.collection(CHAT_CHANNELS).document(chatChannelId)
                    .collection(MESSAGES)
                    .document(itemAtEnd.message.messageId)
                    .get()
                    .addOnSuccessListener {
                        if (it != null && it.exists()) {
                            getMessagesAfter(it)
                        }
                    }.addOnFailureListener {
                        networkErrors.postValue(it)
                    }
            }
        } else {
            getMessagesAfter(lastDoc!!)
        }
    }

    private fun getMessagesBefore(doc: DocumentSnapshot) {
        db.collection(CHAT_CHANNELS).document(chatChannelId)
            .collection(MESSAGES)
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .endAt(doc)
            .limit(50)
            .get()
            .addOnSuccessListener {
                val messages = it.toObjects(SimpleMessage::class.java)
                scope.launch (Dispatchers.IO) {
                    repository.updateMessage(messages)
                }
            }.addOnFailureListener {
                networkErrors.postValue(it)
            }
    }

    private fun getMessagesAfter(doc: DocumentSnapshot) {
        db.collection(CHAT_CHANNELS).document(chatChannelId)
            .collection(MESSAGES)
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .startAfter(doc)
            .limit(50)
            .get()
            .addOnSuccessListener {
                val messages = it.toObjects(SimpleMessage::class.java)
                scope.launch (Dispatchers.IO) {
                    repository.updateMessage(messages)
                    if (messages.size < PAGE_SIZE) {
                        hasReachedEnd = true
                    }
                }
            }.addOnFailureListener {
                networkErrors.postValue(it)
            }
    }

}

