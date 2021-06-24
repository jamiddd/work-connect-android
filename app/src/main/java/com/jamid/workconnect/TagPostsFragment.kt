package com.jamid.workconnect

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.adapter.paging3.PostAdapter
import com.jamid.workconnect.databinding.FragmentTagPostsBinding
import com.jamid.workconnect.interfaces.OnChipClickListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class TagPostsFragment : SupportFragment(R.layout.fragment_tag_posts, TAG, false) {

    private lateinit var binding: FragmentTagPostsBinding
    private lateinit var postAdapter: PostAdapter
    private var job: Job? = null

    private fun getPosts(tag: String) {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getQueryPosts(tag).collectLatest {
                postAdapter.submitData(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTagPostsBinding.bind(view)

        val postsTag = arguments?.getString(ARG_TAG) ?: return
        val fragmentTitle = "#$postsTag"
        viewModel.extras[ARG_TITLE] = fragmentTitle
        binding.tagPostsToolbar.title = fragmentTitle

        initAdapter()

        getPosts(postsTag)

        binding.tagPostsRecycler.setPadding(0, convertDpToPx(8), 0, viewModel.windowInsets.value!!.second)

        initRefresher(postsTag)

        val onChipClickListener = requireContext() as OnChipClickListener

        viewLifecycleOwner.lifecycleScope.launch {
            delay(500)

            val currentUser = viewModel.user.value
            if (currentUser != null) {
                val interests = currentUser.userPrivate.interests
                if (interests.contains(postsTag)) {
                    binding.followTagButton.apply {
                        text = "Unfollow"
                        icon = null

                        setOnClickListener {
                            onChipClickListener.onInterestRemoved(postsTag)
                        }
                    }
                } else {
                    binding.followTagButton.apply {
                        text = "Follow"
                        icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_add_24)

                        setOnClickListener {
                            onChipClickListener.onInterestSelect(postsTag)
                        }
                    }
                }
            }
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.tagPostsToolbar.updateLayout(marginTop = top)
        }

    }

    private fun initRefresher(tag: String) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (activity.resources?.configuration?.isNightModeActive == true) {
                binding.tagPostsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.darkestGrey))
                binding.tagPostsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.purple_200))
            } else {
                binding.tagPostsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.white))
                binding.tagPostsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))
            }
        } else {
            if (activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                binding.tagPostsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.darkerGrey))
                binding.tagPostsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.purple_200))
            } else {
                binding.tagPostsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.white))
                binding.tagPostsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))
            }
        }

        binding.tagPostsRefresher.setOnRefreshListener {
            getPosts(tag)
            hideProgressBar()
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {

            postAdapter.loadStateFlow.collectLatest { loadStates ->
                binding.tagPostsRefresher.isRefreshing = loadStates.refresh is LoadState.Loading
            }

            postAdapter.loadStateFlow.distinctUntilChangedBy { it.refresh }
                .filter { it.refresh is LoadState.NotLoading }
                .collect { binding.tagPostsRecycler.scrollToPosition(0) }

        }
    }

    private fun hideProgressBar()  = viewLifecycleOwner.lifecycleScope.launchWhenCreated {
        delay(3000)
        binding.tagPostsRefresher.isRefreshing = false
    }

    private fun initAdapter() {
        postAdapter = PostAdapter()
        binding.tagPostsRecycler.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(activity)
        }
    }

    companion object {

        const val TAG = "TagPostsFragment"
        const val ARG_TAG = "ARG_TAG"
        const val ARG_TITLE = "ARG_TITLE"

        @JvmStatic
        fun newInstance(tag: String) = TagPostsFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TAG, tag)
            }
        }
    }
}