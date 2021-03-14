package com.jamid.workconnect.data

import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.CHAT_CHANNELS
import com.jamid.workconnect.CREATED_AT
import com.jamid.workconnect.MESSAGES
import com.jamid.workconnect.model.SimpleMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MessageBoundaryCallback(private val chatChannelId: String, private val repository: MainRepository, private val scope: CoroutineScope) : PagedList.BoundaryCallback<SimpleMessage>() {

    private val db = Firebase.firestore
    private val networkErrors = MutableLiveData<Exception>()
    private var lastDoc: DocumentSnapshot? = null
    private var firstDoc: DocumentSnapshot? = null

    companion object {
        fun newInstance(chatChannelId: String, repository: MainRepository, scope: CoroutineScope)
            = MessageBoundaryCallback(chatChannelId, repository, scope)
    }

    override fun onZeroItemsLoaded() {
        super.onZeroItemsLoaded()

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
                    repository.insertMessages(messages)
                    repository.insertKeys(keys)
                }

                if (!it.isEmpty) {
                    firstDoc = it.first()
                    lastDoc = it.last()
                }
            }.addOnFailureListener {
                networkErrors.postValue(it)
            }

    }

    override fun onItemAtFrontLoaded(itemAtFront: SimpleMessage) {
        super.onItemAtFrontLoaded(itemAtFront)

        db.collection(CHAT_CHANNELS).document(chatChannelId)
            .collection(MESSAGES)
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .endAt(firstDoc)
            .limit(50)
            .get()
            .addOnSuccessListener {
                val messages = it.toObjects(SimpleMessage::class.java)
                scope.launch (Dispatchers.IO) {
                    repository.insertMessages(messages)
                }
            }.addOnFailureListener {
                networkErrors.postValue(it)
            }
    }

    override fun onItemAtEndLoaded(itemAtEnd: SimpleMessage) {
        super.onItemAtEndLoaded(itemAtEnd)

        db.collection(CHAT_CHANNELS).document(chatChannelId)
            .collection(MESSAGES)
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .startAfter(lastDoc)
            .limit(50)
            .get()
            .addOnSuccessListener {
                val messages = it.toObjects(SimpleMessage::class.java)
                scope.launch (Dispatchers.IO) {
                    repository.insertMessages(messages)
                }
            }.addOnFailureListener {
                networkErrors.postValue(it)
            }
    }
}