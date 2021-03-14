package com.jamid.workconnect.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.firebase.ui.firestore.paging.LoadingState
import com.firebase.ui.firestore.paging.LoadingState.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.R
import com.jamid.workconnect.USERS
import com.jamid.workconnect.databinding.UserHorizontalLayoutBinding
import com.jamid.workconnect.interfaces.GenericLoadingStateListener
import com.jamid.workconnect.interfaces.UserItemClickListener
import com.jamid.workconnect.model.SearchResult
import com.jamid.workconnect.model.User

class UserHorizontalAdapterAlternative(
    options: FirestorePagingOptions<SearchResult>,
    private val genericLoadingStateListener: GenericLoadingStateListener,
    private val userItemClickListener: UserItemClickListener
) : FirestorePagingAdapter<SearchResult, UserHorizontalAdapterAlternative.UserHorizontalViewHolderAlternative>(options) {
    private val db = Firebase.firestore

    inner class UserHorizontalViewHolderAlternative(val binding: UserHorizontalLayoutBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(searchResult: SearchResult?) {
            if (searchResult != null) {
                val otherUserId = searchResult.id
                db.collection(USERS).document(otherUserId)
                    .get()
                    .addOnSuccessListener {
                        val user = it.toObject(User::class.java)!!
                        binding.userHorizPhoto.setImageURI(user.photo)
                        binding.userHorizName.text = user.name
                        binding.userHorizAbout.text = user.about
                        if (user.about != null) {
                            binding.userHorizAbout.visibility = View.VISIBLE
                        }
                        binding.userHorizName.setBackgroundColor(Color.TRANSPARENT)

                        binding.root.setOnClickListener {
                            userItemClickListener.onUserPressed(user.id)
                        }

                    }.addOnFailureListener {

                    }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserHorizontalViewHolderAlternative {
        return UserHorizontalViewHolderAlternative(DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.user_horizontal_layout, parent, false))
    }

    override fun onBindViewHolder(holder: UserHorizontalViewHolderAlternative, position: Int, model: SearchResult) {
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