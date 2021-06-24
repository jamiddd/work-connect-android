package com.jamid.workconnect.profile

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.InsetControlFragment
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.paging3.PostAdapter
import com.jamid.workconnect.databinding.FragmentBlogsBinding
import com.jamid.workconnect.home.EditorFragment
import com.jamid.workconnect.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(androidx.paging.ExperimentalPagingApi::class)
class BlogsFragment : InsetControlFragment(R.layout.fragment_blogs) {

    private lateinit var binding: FragmentBlogsBinding
    private lateinit var postAdapter: PostAdapter
    private var job: Job? = null

    private fun getUserBlogs(user: User) {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.otherUserBlogsFlow(user).collectLatest {
                postAdapter.submitData(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentBlogsBinding.bind(view)
        val user = arguments?.getParcelable<User>(ARG_USER)

        if (user != null) {

            initAdapter()

            if (user.id == viewModel.user.value?.id) {
                binding.noBlogsCreatePost.visibility = View.VISIBLE
            } else {
                binding.noBlogsText.text = "No blogs"
                binding.noBlogsCreatePost.visibility = View.GONE
            }

            val options = navOptions {
                anim {
                    enter = R.anim.slide_in_right
                    exit = R.anim.slide_out_left
                    popEnter = R.anim.slide_in_left
                    popExit = R.anim.slide_out_right
                }
            }

            binding.noBlogsCreatePost.setOnClickListener {
                findNavController().navigate(R.id.editorFragment, null, options)
//                activity.toFragment(EditorFragment.newInstance(), EditorFragment.TAG)
            }

            setBlogsRefresher(user)

            getUserBlogs(user)
        }
    }

    private fun setBlogsRefresher(user: User) {
        viewLifecycleOwner.lifecycleScope.launch {
            postAdapter.loadStateFlow.collectLatest { loadStates ->
                binding.blogsRefresher.isRefreshing = true
                if (loadStates.refresh is LoadState.NotLoading) {
                    delay(1000)
                    binding.blogsRefresher.isRefreshing = false
                    if (postAdapter.itemCount == 0) {
                        showEmptyNotificationsUI()
                    } else {
                        hideEmptyNotificationsUI()
                    }
                }
            }
        }

        binding.blogsRefresher.setOnRefreshListener {
            getUserBlogs(user)
        }
    }

    private fun hideEmptyNotificationsUI() {
        binding.blogsRecycler.visibility = View.VISIBLE
        binding.noBlogsLayoutScroll.visibility = View.GONE
    }

    private fun showEmptyNotificationsUI() {
        binding.blogsRecycler.visibility = View.GONE
        binding.noBlogsLayoutScroll.visibility = View.VISIBLE
    }

    private fun initAdapter() {
        postAdapter = PostAdapter()

        binding.blogsRecycler.apply {
            adapter = postAdapter
            itemAnimator = null
            setRecycledViewPool(activity.recyclerViewPool)
            layoutManager = LinearLayoutManager(activity)
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