package com.jamid.workconnect.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.workconnect.R
import com.jamid.workconnect.databinding.UserHorizontalLayoutBinding
import com.jamid.workconnect.interfaces.UserItemClickListener
import com.jamid.workconnect.model.ChatChannelContributor

class VerticalContributorsAdapter(private val userItemClickListener: UserItemClickListener): ListAdapter<ChatChannelContributor, VerticalContributorsAdapter.VerticalContributorsViewHolder>(
    COMPARATOR) {

    companion object {
        val COMPARATOR = object : DiffUtil.ItemCallback<ChatChannelContributor>() {
            override fun areItemsTheSame(
                oldItem: ChatChannelContributor,
                newItem: ChatChannelContributor
            ) = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: ChatChannelContributor,
                newItem: ChatChannelContributor
            ) = oldItem == newItem
        }
    }

    inner class VerticalContributorsViewHolder(val binding: UserHorizontalLayoutBinding): RecyclerView.ViewHolder(binding.root) {
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

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VerticalContributorsViewHolder {
        return VerticalContributorsViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.user_horizontal_layout, parent, false))
    }

    override fun onBindViewHolder(holder: VerticalContributorsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}