package com.jamid.workconnect.adapter.paging2

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.workconnect.PROJECT
import com.jamid.workconnect.R
import com.jamid.workconnect.interfaces.SearchItemClickListener
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.User

class SearchItemViewHolder(val view: View, val context: Context): RecyclerView.ViewHolder(view) {

	private val searchItemClickListener = context as SearchItemClickListener

	fun <T> bind(item: T, clazz: Class<T>) {

		val searchImg = view.findViewById<SimpleDraweeView>(R.id.search_img)
		val searchTextResult = view.findViewById<TextView>(R.id.search_text_result)
		val searchAddAction = view.findViewById<Button>(R.id.search_result_action)

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
		}

		view.setOnClickListener {
			searchItemClickListener.onSearchItemClick(item, clazz)
		}
	}
}