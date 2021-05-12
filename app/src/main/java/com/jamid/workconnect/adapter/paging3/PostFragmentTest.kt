package com.jamid.workconnect.adapter.paging3

import android.os.Bundle
import android.view.View
import com.jamid.workconnect.R
import com.jamid.workconnect.SupportFragment
import com.jamid.workconnect.databinding.PostFragTestBinding
import com.jamid.workconnect.model.Post

class PostFragmentTest: SupportFragment(R.layout.post_frag_test, TITLE, false){

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val binding = PostFragTestBinding.bind(view)
		val post = arguments?.getParcelable<Post>(ARG_POST)!!

		binding.postIsLiked.text = post.postLocalData.isLiked.toString()
		binding.postLikesCount.text = post.likes.toString()
		binding.postLikeBtn.setOnClickListener {
			post.postLocalData.isLiked = !post.postLocalData.isLiked
			post.likes = post.likes + 1
			binding.postIsLiked.text = post.postLocalData.isLiked.toString()
			binding.postLikesCount.text = post.likes.toString()
			activity.currentViewHolder?.bind(post)
		}
		binding.postName.text = post.title
	}

	companion object {
		const val TAG = "PostFragmentTest"
		const val TITLE = "Post"
		const val ARG_POST = "ARG_POST"

		fun newInstance(post: Post) = PostFragmentTest().apply {
			arguments = Bundle().apply {
				putParcelable(ARG_POST, post)
			}
		}

	}
}