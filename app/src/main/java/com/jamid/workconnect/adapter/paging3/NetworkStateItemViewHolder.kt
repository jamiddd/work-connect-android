package com.jamid.workconnect.adapter.paging3

import android.view.View
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
import com.jamid.workconnect.databinding.NetworkStateItemBinding

class NetworkStateItemViewHolder (val view: View, private val retryCallback: () -> Unit): RecyclerView.ViewHolder(view) {

	private val binding = NetworkStateItemBinding.bind(view)
	private val progressBar = binding.progressBar
	private val errorMsg = binding.errorMsg
	private val retry = binding.retryButton.also {
		it.setOnClickListener {
			retryCallback()
		}
	}

	fun bind(loadState: LoadState) {
		progressBar.isVisible = loadState is LoadState.Loading
		retry.isVisible = loadState is LoadState.Error
		errorMsg.isVisible = !(loadState as? LoadState.Error)?.error?.message.isNullOrBlank()
		errorMsg.text = (loadState as? LoadState.Error)?.error?.message
	}
}