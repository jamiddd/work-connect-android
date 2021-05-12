package com.jamid.workconnect.profile

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.InsetControlFragment
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.paging2.PostAdapter
import com.jamid.workconnect.databinding.FragmentBlogsBinding
import com.jamid.workconnect.home.EditorFragment
import com.jamid.workconnect.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class BlogsFragment : InsetControlFragment(R.layout.fragment_blogs) {

    private lateinit var binding: FragmentBlogsBinding
    private lateinit var postAdapter: PostAdapter
    private var job: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentBlogsBinding.bind(view)
        val user = arguments?.getParcelable<User>(ARG_USER)

        if (user != null) {

            postAdapter = PostAdapter()

            binding.blogsRecycler.apply {
                adapter = postAdapter
                itemAnimator = null
                layoutManager = LinearLayoutManager(activity)
            }

            OverScrollDecoratorHelper.setUpOverScroll(binding.blogsRecycler, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)


            if (user.id == viewModel.user.value?.id) {
                binding.noBlogsCreatePost.visibility = View.VISIBLE
            } else {
                binding.noBlogsCreatePost.visibility = View.GONE
            }

            binding.noBlogsCreatePost.setOnClickListener {
                activity.toFragment(EditorFragment.newInstance(), EditorFragment.TAG)
            }

            viewModel.userBlogs(user.id).observe(viewLifecycleOwner) {
                if (!it.isNullOrEmpty()) {
                    job?.cancel()
                    activity.mainBinding.primaryProgressBar.visibility = View.GONE
                    binding.blogsRecycler.visibility = View.VISIBLE
                    binding.noBlogsLayoutScroll.visibility = View.GONE
                    postAdapter.submitList(it)
                } else {
                    activity.mainBinding.primaryProgressBar.visibility = View.VISIBLE
                    job = lifecycleScope.launch {
                        delay(1000)
                        activity.mainBinding.primaryProgressBar.visibility = View.GONE
                        binding.blogsRecycler.visibility = View.GONE
                        OverScrollDecoratorHelper.setUpOverScroll(binding.noBlogsLayoutScroll)
                        binding.noBlogsLayoutScroll.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    companion object {

        private const val ARG_USER = "ARG_USER"

        @JvmStatic
        fun newInstance(user: User?) = BlogsFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_USER, user)
            }
        }

    }


}