package com.jamid.workconnect.adapter.paging3

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.paging2.GenericComparator
import com.jamid.workconnect.model.Post

class PostAdapter : PagingDataAdapter<Post, PostViewHolder>(GenericComparator(Post::class.java)){

	override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
		getItem(position)?.let {
			holder.bind(it)
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
		return PostViewHolder.newInstance(parent, R.layout.mini_project_item)
	}

}