package com.jamid.workconnect.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.firebase.ui.firestore.paging.LoadingState
import com.firebase.ui.firestore.paging.LoadingState.*
import com.jamid.workconnect.R
import com.jamid.workconnect.databinding.UserHorizontalLayoutBinding
import com.jamid.workconnect.interfaces.GenericLoadingStateListener
import com.jamid.workconnect.interfaces.UserItemClickListener
import com.jamid.workconnect.model.ChatChannelContributor

class UserHorizontalAdapter(
    options: FirestorePagingOptions<ChatChannelContributor>,
    private val genericLoadingStateListener: GenericLoadingStateListener,
    private val userItemClickListener: UserItemClickListener
): FirestorePagingAdapter<ChatChannelContributor, UserHorizontalAdapter.UserHorizontalViewHolder>(options) {
    inner class UserHorizontalViewHolder(val binding: UserHorizontalLayoutBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(chatChannelContributor: ChatChannelContributor?) {
            if (chatChannelContributor != null) {
                if (chatChannelContributor.admin) {
                    binding.userHorizAbout.text = "Admin"
                    binding.userHorizAbout.visibility = View.VISIBLE
                    binding.userHorizAbout.setTextColor(ContextCompat.getColor(binding.root.context, R.color.darkerGrey))
                }
                binding.userHorizPhoto.setImageURI(chatChannelContributor.photo)
                binding.userHorizName.text = chatChannelContributor.name
                binding.userHorizName.setBackgroundColor(Color.TRANSPARENT)

                binding.root.setOnClickListener {
                    userItemClickListener.onUserPressed(chatChannelContributor.id)
                }

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserHorizontalViewHolder {
        return UserHorizontalViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.user_horizontal_layout, parent, false))
    }

    override fun onBindViewHolder(holder: UserHorizontalViewHolder, position: Int, model: ChatChannelContributor) {
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