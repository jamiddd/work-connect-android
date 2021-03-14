package com.jamid.workconnect.adapter

import android.util.Log
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.NotificationItemBinding
import com.jamid.workconnect.interfaces.GenericLoadingStateListener
import com.jamid.workconnect.interfaces.NotificationClickListener
import com.jamid.workconnect.model.*

class NotificationAdapter(
    options: FirestorePagingOptions<SimpleNotification>,
    private val genericLoadingStateListener: GenericLoadingStateListener,
    private val notificationClickListener: NotificationClickListener
) : FirestorePagingAdapter<SimpleNotification, NotificationAdapter.NotificationViewHolder>(options) {

    /*
    * FOR ACCEPTING
    *
    * 1. UPDATE NOTIFICATION SENDER
    * 1.1 Update chatChannels of the sender
    * 1.2 Update collaborations of the sender
    * 1.3 Delete post id from active requests of the sender
    * 1.4 Add new notification for success
    *
    * 2. UPDATE CONNECTED POST
    * 2.1 Update contributors in post document
    * 2.2 Delete notification sender id from post requests collections
    *
    * 3. UPDATE CONNECTED CHAT CHANNEL
    * 3.1 Update contributors in chat channel document
    * 3.2 Add new contributor in contributors collection for this chat channel
    *
    * 4. UPDATE CURRENT USER
    * 4.1 Delete notification request from collection of current user
    * */


    /*
    * FOR DENYING REQUEST
    *
    * 1. UPDATE NOTIFICATION SENDER
    * 1.1 Delete post id from active requests of the sender
    * 1.2 Add new notification for denial
    *
    * 2. UPDATE CONNECTED POST
    * 2.1 Delete request from post requests collections
    *
    * 3. UPDATE CURRENT USER
    * 3.1 Delete notification request from collection of current user
    * */

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    inner class NotificationViewHolder(val binding: NotificationItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: SimpleNotification?) {
            if (notification != null) {

                if (notification.requestId != null) {
                    binding.positiveBtn.visibility = View.VISIBLE
                    binding.negativeBtn.visibility = View.VISIBLE
                }

                binding.negativeBtn.setOnClickListener {
                    db.runBatch {
                        val rejectNotificationRef = db.collection(USERS).document(notification.senderId)
                            .collection(NOTIFICATIONS)
                            .document()
                        val notificationID = rejectNotificationRef.id
                        val newNotification = SimpleNotification(notificationID, "has denied your request to join project.", notification.postId, notification.senderId, notification.userId, null, null, System.currentTimeMillis())

                        // 1.1
                        it.update(db.collection(USERS).document(notification.senderId), ACTIVE_REQUESTS, FieldValue.arrayRemove(notification.postId))

                        // 1.2
                        it.set(rejectNotificationRef, newNotification)

                        // 2.1
                        it.delete(db.collection(POSTS).document(notification.postId)
                            .collection(REQUESTS)
                            .document(notification.requestId!!))

                        // 3.1
                        it.delete(db.collection(USERS).document(auth.currentUser!!.uid)
                            .collection(NOTIFICATIONS)
                            .document(notification.id))

                    }.addOnSuccessListener {
                        refresh()
                    }.addOnFailureListener {
                        Log.e("NotificationAdapter", it.localizedMessage)
                    }
                }

                binding.positiveBtn.setOnClickListener {

                    // start showing UI progress

                    db.runTransaction {

                        val snap = it.get(db.collection(USER_MINIMALS).document(notification.senderId))
                        val user = snap.toObject(UserMinimal::class.java)!!
                        val senderRef = db.collection(USERS).document(notification.senderId)

                        val acceptNotificationRef = senderRef.collection(NOTIFICATIONS).document()
                        val notificationID = acceptNotificationRef.id
                        val newNotification = SimpleNotification(notificationID, "has accepted your request to join project.", notification.postId, notification.senderId, notification.userId, null, null, System.currentTimeMillis())

                        // 1.1
                        it.update(senderRef, CHAT_CHANNELS, FieldValue.arrayUnion(notification.postId))

                        // 1.2
                        it.update(senderRef, COLLABORATION_IDS, FieldValue.arrayUnion(notification.postId))

                        // 1.3
                        it.update(senderRef, ACTIVE_REQUESTS, FieldValue.arrayRemove(notification.postId))

                        // 1.4
                        it.set(acceptNotificationRef, newNotification)


                        // 2.1
                        val postRef = db.collection(POSTS).document(notification.postId)
                        it.update(postRef, CONTRIBUTORS, FieldValue.arrayUnion(notification.senderId))

                        // 2.2
                        it.delete(postRef.collection(REQUESTS).document(notification.requestId!!))

                        // 3.1
                        val chatChannelRef = db.collection(CHAT_CHANNELS).document(notification.chatChannelId!!)
                        it.update(chatChannelRef, CONTRIBUTORS_LIST, FieldValue.arrayUnion(user.id))

                        // 3.2
                        val chatChannelContributor = ChatChannelContributor(notification.senderId, user.name, user.username, user.photo, false)
                        it.set(chatChannelRef.collection(CONTRIBUTORS).document(notification.senderId), chatChannelContributor)

                        // 4.1
                        it.delete(db.collection(USERS).document(auth.currentUser!!.uid)
                            .collection(NOTIFICATIONS)
                            .document(notification.id))
                    }.addOnSuccessListener {
                        refresh()
                    }.addOnFailureListener {
                        Log.e("NotificationAdapter", it.localizedMessage)
                    }
                }

                db.collection(POSTS).document(notification.postId).get()
                    .addOnSuccessListener {
                        if (it != null && it.exists()) {
                            val post = it.toObject(Post::class.java)!!
                            binding.projectNameText.text = post.title

                            binding.root.setOnClickListener {
                                notificationClickListener.onItemClick(post)
                            }

                        }
                    }.addOnFailureListener {
                        Log.e("NotificationAdapter", it.localizedMessage)
                    }

                db.collection(USERS).document(notification.senderId).get()
                    .addOnSuccessListener {
                        if (it != null && it.exists()) {
                            val sender = it.toObject(User::class.java)!!
                            binding.relevantImgView.setImageURI(sender.photo)
                            binding.requestContentText.text = sender.name + " " + notification.content
                        }
                    }.addOnFailureListener {
                        Log.e("NotificationAdapter", it.localizedMessage)
                    }

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        return NotificationViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.notification_item, parent, false))
    }

    override fun onBindViewHolder(
        holder: NotificationViewHolder,
        position: Int,
        model: SimpleNotification
    ) {
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