package com.jamid.workconnect.adapter.paging3

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import com.jamid.workconnect.R

class PostsLoadStateAdapter(private val adapter: PostAdapter): LoadStateAdapter<NetworkStateItemViewHolder>(){
	override fun onBindViewHolder(holder: NetworkStateItemViewHolder, loadState: LoadState) {
		holder.bind(loadState)
	}

	override fun onCreateViewHolder(
		parent: ViewGroup,
		loadState: LoadState
	): NetworkStateItemViewHolder {
		return NetworkStateItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.network_state_item, parent, false)){adapter.retry()}
	}

}