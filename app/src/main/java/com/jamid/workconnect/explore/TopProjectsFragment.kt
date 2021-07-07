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
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.paging3.PostAdapter
import com.jamid.workconnect.adapter.paging3.PostsLoadStateAdapter
import com.jamid.workconnect.databinding.FragmentTopProjectsBinding
import com.jamid.workconnect.model.Post
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class TopProjectsFragment : PagingListFragment(R.layout.fragment_top_projects) {

    private lateinit var binding: FragmentTopProjectsBinding
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

        topPostsAdapter = PostAdapter()

        binding.allTopProjectsRecycler.setListAdapter(pagingAdapter = topPostsAdapter, clazz = Post::class.java, onComplete = {
            getTopProjects()
        })

        binding.topProjectsToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.allTopProjectsRefresher.setSwipeRefresher {
            getTopProjects()
            stopRefreshProgress(it)
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.topProjectsToolbar.updateLayout(marginTop = top)
            binding.allTopProjectsRecycler.setPadding(0, convertDpToPx(8), 0, bottom + convertDpToPx(56))
        }

    }

    companion object {

        const val TAG = "TopProjectsFragment"
        const val TITLE = "Top Projects"

        @JvmStatic
        fun newInstance() =
            TopProjectsFragment()
    }
}