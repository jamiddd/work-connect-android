package com.jamid.workconnect.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.R
import com.jamid.workconnect.StringComparator
import com.jamid.workconnect.databinding.NotificationItemBinding
import com.jamid.workconnect.interfaces.RequestItemClickListener
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.SimpleRequest

class ActiveRequestsAdapter(val isSent: Boolean, val requestItemClickListener: RequestItemClickListener): ListAdapter<String, ActiveRequestsAdapter.ActiveRequestViewHolder>(StringComparator()) {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    inner class ActiveRequestViewHolder(val binding: NotificationItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(postId: String) {
            if (isSent) {
                // make necessary ui changes
                binding.positiveBtn.text = "Undo"
                binding.positiveBtn.visibility = View.VISIBLE


                db.collection("posts").document(postId).get()
                    .addOnSuccessListener {
                        if (it != null && it.exists()) {
                            val post = it.toObject(Post::class.java)!!
                            binding.relevantImgView.setImageURI(post.thumbnail)
                            binding.projectNameText.text = post.title

                            binding.requestContentText.text = "Your request has not been accepted yet."

                            binding.root.setOnClickListener {
                                requestItemClickListener.onRequestItemClick(post)
                            }
                        }
                    }.addOnFailureListener { e ->
                        Log.e("ActiveRequestAdapter", e.message.toString())
                    }


                // get the request item

                db.collection("posts").document(postId).collection("requests")
                    .whereEqualTo("sender", auth.currentUser!!.uid)
                    .limit(1)
                    .get()
                    .addOnSuccessListener {
                        if (it != null && it.isEmpty) {
                            return@addOnSuccessListener
                        }

                        val request = it.documents[0].toObject(SimpleRequest::class.java)!!

                        binding.positiveBtn.setOnClickListener {
                            val requestId = request.id
                            val receiverId = request.receiver
                            val notificationId = request.notificationId

                            db.runBatch { it1 ->


                                // 1
                                it1.delete(db.collection("posts").document(postId).collection("requests")
                                    .document(requestId))

                                // 2
                                it1.update(db.collection("users").document(auth.currentUser!!.uid), "activeRequests", FieldValue.arrayRemove(postId))

                                // 3
                                it1.delete(db.collection("users").document(receiverId).collection("notifications").document(notificationId))

                                // 4
//                                it1.update(db.collection("chatChannels").document(postMutable!!.chatChannelId), "contributorsList", FieldValue.arrayUnion(request.sender))

                                // 5
//                                it1.update(db.collection("posts").document(postId), "contributors", FieldValue.arrayUnion(request.sender))

                            }.addOnSuccessListener {
                                requestItemClickListener.onDelete(postId, layoutPosition)
                            }.addOnFailureListener { e ->
                                Log.e("ActiveRequestAdapter", e.message.toString())
                            }
                        }
                    }.addOnFailureListener { e ->
                        Log.e("ActiveRequestAdapter", e.message.toString())
                    }
            } else {
                TODO("What happens on receive")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveRequestViewHolder {
        return ActiveRequestViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.notification_item, parent, false))
    }

    override fun onBindViewHolder(holder: ActiveRequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}