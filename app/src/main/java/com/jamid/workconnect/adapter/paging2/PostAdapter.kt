package com.jamid.workconnect.adapter.paging2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import com.jamid.workconnect.R
import com.jamid.workconnect.model.Post

class PostAdapter: PagedListAdapter<Post, PostViewHolder>(GenericComparator(Post::class.java)) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        return PostViewHolder.newInstance(
	        LayoutInflater.from(parent.context).inflate(R.layout.mini_project_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}