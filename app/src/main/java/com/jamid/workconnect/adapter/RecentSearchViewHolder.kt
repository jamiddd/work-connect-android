package com.jamid.workconnect.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.button.MaterialButton
import com.jamid.workconnect.BLOG
import com.jamid.workconnect.PROJECT
import com.jamid.workconnect.R
import com.jamid.workconnect.USER
import com.jamid.workconnect.interfaces.SearchItemClickListener
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.RecentSearch
import com.jamid.workconnect.model.User

class RecentSearchViewHolder(parent: ViewGroup, @LayoutRes layout: Int): GenericViewHolder<RecentSearch>(parent, layout) {

    private val view = itemView
    private val searchItemClickListener = view.context as SearchItemClickListener

    override fun bind(item: RecentSearch) {

        val searchImg = view.findViewById<SimpleDraweeView>(R.id.search_img)
        val searchTextResult = view.findViewById<TextView>(R.id.search_text_result)
        val searchAddAction = view.findViewById<MaterialButton>(R.id.search_result_action)

        searchAddAction.rotation = 0f
        searchAddAction.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_baseline_close_24)

        when (item.type) {
            USER -> {
                val user = item.recentUser

                if (user != null) {

                    searchImg.visibility = View.VISIBLE
                    searchImg.setImageURI(user.photo)

                    searchTextResult.text = user.name

                    searchAddAction.setOnClickListener {
                        searchItemClickListener.onDeleteRecentSearch(user.name)
                    }
                }
            }
            BLOG, PROJECT -> {
                val post = item.recentPost
                if (post != null) {
                    searchTextResult.text = post.title
                    if (post.type == PROJECT) {
                        searchImg.visibility = View.VISIBLE
                        searchImg.setImageURI(post.thumbnail)
                    } else {
                        searchImg.visibility = View.INVISIBLE
                    }
                    searchAddAction.setOnClickListener {
                        searchItemClickListener.onDeleteRecentSearch(post.title)
                    }
                }
            }
        }

        view.setOnClickListener {
            if (item.recentUser != null) {
                searchItemClickListener.onSearchItemClick(item.recentUser, User::class.java)
            } else if (item.recentPost != null) {
                searchItemClickListener.onSearchItemClick(item.recentPost, Post::class.java)
            }
        }
    }

    companion object {
        fun newInstance(parent: ViewGroup, @LayoutRes layout: Int): RecentSearchViewHolder {
            return RecentSearchViewHolder(parent, layout)
        }
    }

}