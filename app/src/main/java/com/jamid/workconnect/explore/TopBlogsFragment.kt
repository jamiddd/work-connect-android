package com.jamid.workconnect.explore

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.R
import com.jamid.workconnect.SupportFragment
import com.jamid.workconnect.adapter.paging3.PostAdapter
import com.jamid.workconnect.adapter.paging3.PostsLoadStateAdapter
import com.jamid.workconnect.databinding.FragmentTopBlogsBinding
import com.jamid.workconnect.updateLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class TopBlogsFragment : SupportFragment(R.layout.fragment_top_blogs, TAG, false) {

    private lateinit var binding: FragmentTopBlogsBinding
    private lateinit var topPostsAdapter: PostAdapter
    private var job: Job? = null

    private fun getTopBlogs() {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.topBlogsFlow().collectLatest {
                topPostsAdapter.submitData(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTopBlogsBinding.bind(view)

        activity.currentFeedFragment = this

        binding.topBlogsToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        initAdapter()

        initRefresher()

        getTopBlogs()

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.topBlogsToolbar.updateLayout(marginTop = top)
        }

    }

    private fun initAdapter() {

        topPostsAdapter = PostAdapter()
        activity.currentAdapter = topPostsAdapter

        binding.allTopBlogsRecycler.apply {
            setRecycledViewPool(activity.recyclerViewPool)
            adapter = topPostsAdapter.withLoadStateFooter(
                footer = PostsLoadStateAdapter(topPostsAdapter)
            )
            itemAnimator = null
            layoutManager = LinearLayoutManager(activity)
        }
    }


    private fun initRefresher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (activity.resources?.configuration?.isNightModeActive == true) {
                binding.allTopBlogsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.darkestGrey))
                binding.allTopBlogsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.purple_200))
            } else {
                binding.allTopBlogsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.white))
                binding.allTopBlogsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))
            }
        } else {
            if (activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                binding.allTopBlogsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.darkerGrey))
                binding.allTopBlogsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.purple_200))
            } else {
                binding.allTopBlogsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.white))
                binding.allTopBlogsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))
            }
        }

        binding.allTopBlogsRefresher.setOnRefreshListener {
            getTopBlogs()
            hideProgressBar()
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {

            topPostsAdapter.loadStateFlow.collectLatest { loadStates ->
                binding.allTopBlogsRefresher.isRefreshing = loadStates.refresh is LoadState.Loading
            }

            topPostsAdapter.loadStateFlow.distinctUntilChangedBy { it.refresh }
                .filter { it.refresh is LoadState.NotLoading }
                .collect { binding.allTopBlogsRecycler.scrollToPosition(0) }

        }
    }

    private fun hideProgressBar()  = viewLifecycleOwner.lifecycleScope.launch {
        delay(5000)
        binding.allTopBlogsRefresher.isRefreshing = false
    }

    companion object {

        const val TAG = "TopBlogsFragment"
        const val TITLE = "Top Blogs"

        @JvmStatic
        fun newInstance() =
            TopBlogsFragment().apply {
                arguments = Bundle()
            }
    }
}