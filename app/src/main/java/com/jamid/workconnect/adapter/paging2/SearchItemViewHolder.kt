package com.jamid.workconnect.adapter.paging2

import android.content.Context
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
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

class SearchItemViewHolder(val view: View, val context: Context): RecyclerView.ViewHolder(view) {

	private val searchItemClickListener = context as SearchItemClickListener

	fun <T> bind(item: T?, clazz: Class<T>) {
		if (item != null) {
			val searchImg = view.findViewById<SimpleDraweeView>(R.id.search_img)
			val searchTextResult = view.findViewById<TextView>(R.id.search_text_result)
			val searchAddAction = view.findViewById<MaterialButton>(R.id.search_result_action)

			if (Build.VERSION.SDK_INT <= 27) {
				val colorBlack = ContextCompat.getColor(view.context, R.color.black)
				searchTextResult.setTextColor(colorBlack)
			}

			view.setOnClickListener {
				searchItemClickListener.onSearchItemClick(item, clazz)
			}

			when (clazz) {
				User::class.java -> {
					val user = item as User?

					if (user != null) {

						searchImg.visibility = View.VISIBLE
						searchImg.setImageURI(user.photo)

						searchTextResult.text = user.name

						searchAddAction.setOnClickListener {
							searchItemClickListener.onSearchAdded(user.name)
						}
					}
				}
				Post::class.java -> {
					val post = item as Post?
					if (post != null) {
						searchTextResult.text = post.title
						if (post.type == PROJECT) {
							searchImg.visibility = View.VISIBLE
							searchImg.setImageURI(post.thumbnail)
						} else {
							searchImg.visibility = View.INVISIBLE
						}
						searchAddAction.setOnClickListener {
							searchItemClickListener.onSearchAdded(post.title)
						}
					}
				}
				RecentSearch::class.java -> {
					val recentSearch = item as RecentSearch
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
			}
		}
	}
}