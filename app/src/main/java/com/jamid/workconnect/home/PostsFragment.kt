package com.jamid.workconnect.home

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.paging3.PostAdapter
import com.jamid.workconnect.adapter.paging3.PostsLoadStateAdapter
import com.jamid.workconnect.databinding.FragmentProjectsBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

@ExperimentalPagingApi
class PostsFragment : InsetControlFragment(R.layout.fragment_projects) {

    private lateinit var binding: FragmentProjectsBinding
    private lateinit var postAdapter: PostAdapter
    private var job: Job? = null

    private fun getPosts(tag: String? = null) {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            viewModel.postsFlow(viewLifecycleOwner.lifecycleScope, tag).collectLatest {
                postAdapter.submitData(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProjectsBinding.bind(view)

        val sharedPreference = activity.getSharedPreferences(WORK_CONNECT_SHARED_PREF, MODE_PRIVATE)

        setInsetView(binding.projectsFragmentList, mapOf(INSET_TOP to 112, INSET_BOTTOM to 64))
        OverScrollDecoratorHelper.setUpOverScroll(binding.noPostsLayoutScroll)

        postAdapter = PostAdapter()

        binding.projectsFragmentList.apply {
            setRecycledViewPool(activity.recyclerViewPool)
            adapter = postAdapter.withLoadStateHeaderAndFooter(
                header = PostsLoadStateAdapter(postAdapter),
                footer = PostsLoadStateAdapter(postAdapter)
            )
            itemAnimator = null
            layoutManager = LinearLayoutManager(activity)
        }

        binding.noPostsExplore.setOnClickListener {
            activity.mainBinding.bottomNav.selectedItemId = R.id.explore_navigation
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            postAdapter.loadStateFlow.collectLatest { loadStates ->
                binding.postsRefresher.isRefreshing = loadStates.refresh is LoadState.Loading
            }
        }

        /*viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            viewModel.postsFlow().collectLatest {
                pAdapter.submitData(it)
            }
        }*/


        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            postAdapter.loadStateFlow.distinctUntilChangedBy { it.refresh }
                .filter { it.refresh is LoadState.NotLoading }
                .collect { binding.projectsFragmentList.scrollToPosition(0) }
        }

        viewModel.miniUser.observe(viewLifecycleOwner) {
            if (it != null) {
                if (!binding.postsRefresher.isRefreshing) {
                    Log.d(BUG_TAG, "Posts refresher is not refreshing at the moment - inside mini user")
                    postAdapter.refresh()
                }
            }
        }

        /*viewModel.miniUser.observe(viewLifecycleOwner) {
            // trigger for user change
            if (it != null) {
                // only then get posts
                val map = mapOf(HOME_FEED to true, WITH_TAG to viewModel.currentFragmentTag.value)
                job?.cancel()
                job = posts(map)

                setTagObserve(isRequestInPlace = true)
            }
        }

        setTagObserve()*/

        binding.postsRefresher.setProgressViewOffset(false, 0, viewModel.windowInsets.value!!.first + convertDpToPx(132))
        binding.postsRefresher.setSlingshotDistance(convertDpToPx(80))
        binding.postsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))

        binding.postsRefresher.setOnRefreshListener {
            getPosts(viewModel.currentHomeTag.value)
            viewLifecycleOwner.lifecycleScope.launchWhenCreated {
                delay(5000)
                binding.postsRefresher.isRefreshing = false
            }
        }

        binding.postsNestedScroll.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (oldScrollY > scrollY) {
                if (activity.mainBinding.primaryAppBar.translationY != 0f) {
                    activity.mainBinding.primaryToolbarContainer.visibility = View.VISIBLE
                    activity.mainBinding.primaryAppBar.translationY = 0f
                }
            } else {
                if (activity.mainBinding.primaryAppBar.translationY == 0f && scrollY > convertDpToPx(100)) {
                    activity.mainBinding.primaryAppBar.translationY = -convertDpToPx(56f)
                    activity.mainBinding.primaryToolbarContainer.visibility = View.INVISIBLE
                }
            }
        }

        viewModel.currentHomeTag.observe(viewLifecycleOwner) {
            if (it != null) {
                getPosts(it)
            } else {
                getPosts()
            }
        }

        if (!sharedPreference.getBoolean(IS_FIRST_TIME, true)) {
            binding.postsRefresher.isRefreshing = true
            getPosts()
        } else {
            getPosts()
            viewLifecycleOwner.lifecycleScope.launchWhenCreated {
                delay(2000)
                val editor = sharedPreference.edit()
                editor.putBoolean(IS_FIRST_TIME, false)
                editor.apply()
                if (!binding.postsRefresher.isRefreshing) {
                    Log.d(BUG_TAG, "Posts refresher is not refreshing at the moment - inside first time")
                    postAdapter.refresh()
                }
            }
        }
    }


    companion object {

        private const val TAG = "PostsFragment"

        @JvmStatic
        fun newInstance() = PostsFragment()
    }

}