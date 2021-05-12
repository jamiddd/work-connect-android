package com.jamid.workconnect.adapter.paging2

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import com.jamid.workconnect.PROJECT
import com.jamid.workconnect.R
import com.jamid.workconnect.model.Post

class PostAdapterHorizontal(
    private val actContext: Context
): PagedListAdapter<Post, PostViewHolderHorizontal>(GenericComparator(Post::class.java)) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolderHorizontal {
        return if (viewType == 0) {
            PostViewHolderHorizontal(LayoutInflater.from(parent.context).inflate(R.layout.micro_project_item, parent, false), actContext)
        } else {
            PostViewHolderHorizontal(LayoutInflater.from(parent.context).inflate(R.layout.micro_blog_item, parent, false), actContext)
        }
    }

    override fun onBindViewHolder(holder: PostViewHolderHorizontal, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return if (item != null) {
            if (item.type == PROJECT) 0 else 1
        } else {
            super.getItemViewType(position)
        }
    }
}