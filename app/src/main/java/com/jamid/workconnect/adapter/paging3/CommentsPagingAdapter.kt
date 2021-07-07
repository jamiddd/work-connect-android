package com.jamid.workconnect.adapter.paging3

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.CommentViewHolder
import com.jamid.workconnect.model.SimpleComment

class CommentsPagingAdapter: PagingDataAdapter<SimpleComment, CommentViewHolder>(GenericComparator2(SimpleComment::class.java)) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        return CommentViewHolder.newInstance(parent, R.layout.comment_item, null, false)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }

}