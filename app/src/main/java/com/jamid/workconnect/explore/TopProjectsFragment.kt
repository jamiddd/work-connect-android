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
import com.jamid.workconnect.databinding.FragmentTopProjectsBinding
import com.jamid.workconnect.updateLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class TopProjectsFragment : SupportFragment(R.layout.fragment_top_projects, "", false) {

    private lateinit var binding: FragmentTopProjectsBinding
    private var job: Job? = null
    private lateinit var topPostsAdapter: PostAdapter

    private fun getTopProjects() {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.topProjectsFlow().collectLatest {
                topPostsAdapter.submitData(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTopProjectsBinding.bind(view)

//        setInsetView(binding.allTopProjectsRecycler, mapOf(insetTop to 0, insetBottom to 8))
        activity.currentFeedFragment = this
        initAdapter()

        binding.topProjectsToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        initRefresher()

        getTopProjects()

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.topProjectsToolbar.updateLayout(marginTop = top)
        }

    }

    private fun initAdapter() {

        topPostsAdapter = PostAdapter()
        activity.currentAdapter = topPostsAdapter

        binding.allTopProjectsRecycler.apply {
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
                binding.allTopProjectsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.darkestGrey))
                binding.allTopProjectsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.purple_200))
            } else {
                binding.allTopProjectsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.white))
                binding.allTopProjectsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))
            }
        } else {
            if (activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                binding.allTopProjectsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.darkerGrey))
                binding.allTopProjectsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.purple_200))
            } else {
                binding.allTopProjectsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.white))
                binding.allTopProjectsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))
            }
        }

        binding.allTopProjectsRefresher.setOnRefreshListener {
            getTopProjects()
            hideProgressBar()
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {

            topPostsAdapter.loadStateFlow.collectLatest { loadStates ->
                binding.allTopProjectsRefresher.isRefreshing = loadStates.refresh is LoadState.Loading
            }

            topPostsAdapter.loadStateFlow.distinctUntilChangedBy { it.refresh }
                .filter { it.refresh is LoadState.NotLoading }
                .collect { binding.allTopProjectsRecycler.scrollToPosition(0) }

        }
    }

    private fun hideProgressBar()  = viewLifecycleOwner.lifecycleScope.launch {
        delay(5000)
        binding.allTopProjectsRefresher.isRefreshing = false
    }

    companion object {

        const val TAG = "TopProjectsFragment"
        const val TITLE = "Top Projects"

        @JvmStatic
        fun newInstance() =
            TopProjectsFragment().apply {
                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
                }
            }
    }
}